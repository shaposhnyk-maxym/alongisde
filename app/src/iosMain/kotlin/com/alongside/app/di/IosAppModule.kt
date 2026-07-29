package com.alongside.app.di

import com.alongside.app.work.NoOpBackgroundWorkScheduler
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.authSessionCache
import com.alongside.core.database.getDatabaseBuilder
import com.alongside.core.database.getRoomDatabase
import com.alongside.core.database.onboardingCompletionCache
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.auth.AuthSessionRepository
import com.alongside.core.domain.diary.processing.EpisodeVisionDescriptionClient
import com.alongside.core.domain.diary.processing.PhotoUploadClient
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.domain.onboarding.OnboardingCompletionCache
import com.alongside.core.domain.pairing.DefaultPairingRepository
import com.alongside.core.domain.pairing.InviteCodeGenerator
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.pretrip.PreTripPhotoUploadClient
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.network.auth.FirebaseAuthApi
import com.alongside.core.network.auth.FirebaseAuthConfig
import com.alongside.core.network.auth.FirebaseAuthSessionRepository
import com.alongside.core.network.auth.SessionFirestoreTokenProvider
import com.alongside.core.network.auth.asIdTokenRefresher
import com.alongside.core.network.client.createFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.gemini.GeminiConfig
import com.alongside.core.network.gemini.GeminiVisionApi
import com.alongside.core.network.gemini.GeminiVisionDescriptionClient
import com.alongside.core.network.places.GooglePlacesConfig
import com.alongside.core.network.places.GooglePlacesGeocodingApi
import com.alongside.core.network.places.GooglePlacesGeocodingClient
import com.alongside.core.network.storage.FirebasePreTripPhotoUploadClient
import com.alongside.core.network.storage.FirebaseStorageApi
import com.alongside.core.network.storage.FirebaseStorageConfig
import com.alongside.core.network.storage.FirebaseStorageUploadClient
import com.alongside.feature.diary.capture.ExifPhotoReader
import com.alongside.feature.diary.capture.IosExifPhotoReader
import com.alongside.feature.diary.capture.IosPhotoByteReader
import com.alongside.feature.diary.capture.IosPhotoCompressor
import com.alongside.feature.diary.capture.PhotoByteReader
import com.alongside.feature.diary.capture.PhotoCompressor
import org.koin.dsl.module

/**
 * Bootstrap-scoped iOS DI module - the minimum needed to get the Login -> Onboarding -> Pairing
 * gate, Home tab, and now Timeline/diary capture rendering in Simulator.
 *
 * `diaryFeatureModule` (wired in `MainViewController.kt`'s `initKoin()`, a separate file from this
 * one) eagerly builds the whole capture/processing chain the moment the Timeline tab is opened
 * (`DiaryTimelineContainer` -> `DiaryTimelineDataSource` -> `DiaryCaptureCoordinator`/
 * `PreTripPhotoCaptureCoordinator` -> `EpisodeProcessingPipeline`), not just a read path -
 * `DiaryModule.kt`'s `single { }` blocks call `get<PhotoCompressor>()`/`get<PlaceGeocodingClient>()`/
 * etc. eagerly, before any photo is actually captured - confirmed live 2026-07-30 (Timeline crashed
 * with a Koin `NoDefinitionFoundException` for exactly these types until the bindings below were
 * added).
 *
 * Deliberately still NOT ported (see docs/roadmap.md M10's iOS TODO and M13.2's Share Extension
 * note): the Places share-link import bindings (`ShareLinkRedirectResolver`,
 * `PlaceDetailsLookupClient`, `PlacePhotoClient`, `PlacePhotoUploadClient`, `PlaceImportPipeline`)
 * - those are a separate feature (M13.2's share-link import), not on the Timeline's dependency
 * chain, so they don't block Timeline/diary capture.
 *
 * [firebaseApiKey]/[firebaseProjectId]/[storageBucket] mirror the same Firebase project's values
 * committed in `androidApp/google-services.json` - there is no iOS-side equivalent file to read
 * these from since this project talks to Firebase over REST (Ktor), never the native SDK (CLAUDE.md
 * ADR #3), so they're just passed in as plain constants. [googlePlacesApiKey]/[geminiApiKey] are
 * NOT safe to hardcode this way (billing/quota-sensitive, unlike the Firebase key) - they come from
 * `iosApp/Secrets.xcconfig` (gitignored, see docs/local-setup.md), read via `NSBundle` in
 * `MainViewController.kt` and passed in here, mirroring Android's `local.properties` ->
 * `BuildConfig` -> `androidAppModule(...)` parameter pattern.
 */
public fun iosAppModule(
    firebaseApiKey: String,
    firebaseProjectId: String,
    storageBucket: String,
    googlePlacesApiKey: String,
    geminiApiKey: String,
) = module {
    single { createFirestoreHttpClient() }
    single { FirebaseAuthConfig(apiKey = firebaseApiKey) }
    single { FirestoreConfig(projectId = firebaseProjectId) }
    single { FirebaseAuthApi(get(), get()) }
    single<AuthSessionRepository> { FirebaseAuthSessionRepository(get()) }
    single { getRoomDatabase(getDatabaseBuilder()) }
    single<AuthSessionCache> { get<AlongsideDatabase>().authSessionCache() }
    single<OnboardingCompletionCache> { get<AlongsideDatabase>().onboardingCompletionCache() }
    single<FirestoreTokenProvider> {
        SessionFirestoreTokenProvider(get(), get<FirebaseAuthApi>().asIdTokenRefresher())
    }
    single { InviteCodeGenerator() }
    single<BackgroundWorkScheduler> { NoOpBackgroundWorkScheduler() }
    single<PairingRepository> { DefaultPairingRepository(get(), get()) }
    single<ExifPhotoReader> { IosExifPhotoReader() }
    single<PhotoByteReader> { IosPhotoByteReader() }
    single<PhotoCompressor> { IosPhotoCompressor() }
    single { GooglePlacesConfig(apiKey = googlePlacesApiKey) }
    single { GooglePlacesGeocodingApi(get(), get()) }
    single<PlaceGeocodingClient> { GooglePlacesGeocodingClient(get()) }
    single { GeminiConfig(apiKey = geminiApiKey) }
    single { GeminiVisionApi(get(), get()) }
    single<EpisodeVisionDescriptionClient> { GeminiVisionDescriptionClient(get()) }
    single { FirebaseStorageConfig(bucket = storageBucket) }
    single { FirebaseStorageApi(get(), get(), get()) }
    single<PhotoUploadClient> {
        val photoCompressor = get<PhotoCompressor>()
        FirebaseStorageUploadClient(
            api = get(),
            config = get(),
            compress = { bytes -> photoCompressor.compress(bytes) },
        )
    }
    single<PreTripPhotoUploadClient> { FirebasePreTripPhotoUploadClient(get(), get()) }
}
