package com.alongside.app.di

import com.alongside.app.work.NoOpBackgroundWorkScheduler
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.authSessionCache
import com.alongside.core.database.getDatabaseBuilder
import com.alongside.core.database.getRoomDatabase
import com.alongside.core.database.onboardingCompletionCache
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.auth.AuthSessionRepository
import com.alongside.core.domain.onboarding.OnboardingCompletionCache
import com.alongside.core.domain.pairing.DefaultPairingRepository
import com.alongside.core.domain.pairing.InviteCodeGenerator
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.network.auth.FirebaseAuthApi
import com.alongside.core.network.auth.FirebaseAuthConfig
import com.alongside.core.network.auth.FirebaseAuthSessionRepository
import com.alongside.core.network.auth.SessionFirestoreTokenProvider
import com.alongside.core.network.auth.asIdTokenRefresher
import com.alongside.core.network.client.createFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.feature.diary.capture.ExifPhotoReader
import com.alongside.feature.diary.capture.IosExifPhotoReader
import com.alongside.feature.diary.capture.IosPhotoByteReader
import com.alongside.feature.diary.capture.PhotoByteReader
import org.koin.dsl.module

/**
 * Bootstrap-scoped iOS DI module - the minimum needed to get the Login -> Onboarding -> Pairing
 * gate and Home tab rendering in Simulator, ahead of docs/roadmap.md M7.
 *
 * Deliberately NOT ported yet (see docs/roadmap.md M10's iOS TODO and M13.2's Share Extension
 * note): GooglePlaces/Gemini config, PhotoCompressor, PhotoUploadClient, FirebaseStorage, and the
 * Places share-link import bindings - and `diaryFeatureModule` itself isn't included in this
 * module's graph yet, so [ExifPhotoReader]/[PhotoByteReader] below aren't reachable from any
 * consumer until a diary capture entry point exists on iOS. Places import will still fail on iOS
 * with a Koin NoDefinitionFoundException until those land - everything else (auth, onboarding
 * gate, pairing, timeline read path, settings, recap) only depends on bindings already present
 * below.
 *
 * [firebaseApiKey]/[firebaseProjectId] mirror the same Firebase project's values committed in
 * `androidApp/google-services.json` - there is no iOS-side equivalent file to read these from
 * since this project talks to Firebase over REST (Ktor), never the native SDK (CLAUDE.md ADR #3),
 * so they're just passed in as plain constants.
 */
public fun iosAppModule(
    firebaseApiKey: String,
    firebaseProjectId: String,
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
}
