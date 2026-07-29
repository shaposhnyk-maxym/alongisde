package com.alongside.app

import androidx.compose.ui.window.ComposeUIViewController
import com.alongside.app.di.appModule
import com.alongside.app.di.iosAppModule
import com.alongside.app.share.currentIosPendingShareText
import com.alongside.app.share.setIosPendingShareText
import com.alongside.data.di.dataModule
import com.alongside.feature.auth.GoogleAuthProvider
import com.alongside.feature.auth.di.authFeatureModule
import com.alongside.feature.diary.di.diaryFeatureModule
import com.alongside.feature.matcher.di.matcherFeatureModule
import com.alongside.feature.onboarding.IosPermissionController
import com.alongside.feature.onboarding.SharePlatform
import com.alongside.feature.onboarding.di.onboardingFeatureModule
import com.alongside.feature.pairing.di.pairingFeatureModule
import com.alongside.feature.places.di.placesFeatureModule
import com.alongside.feature.recap.di.recapFeatureModule
import com.alongside.feature.settings.di.settingsFeatureModule
import org.koin.core.context.startKoin
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/**
 * Same Firebase project as `androidApp/google-services.json` - see [iosAppModule]'s kdoc for why
 * these are plain constants here instead of a parsed config file.
 */
private const val FIREBASE_API_KEY = "AIzaSyBngsPMdA2piAYsJNTTAzVjuA5hbzfX9HY"
private const val FIREBASE_PROJECT_ID = "alongside-b05f2"
private const val FIREBASE_STORAGE_BUCKET = "alongside-b05f2.firebasestorage.app"

/**
 * Unlike the Firebase constants above, these come from `Info.plist` (baked in at build time from
 * `iosApp/Secrets.xcconfig`, gitignored - see [iosAppModule]'s kdoc) rather than being hardcoded
 * here, since they're billing/quota-sensitive.
 */
private fun infoPlistString(key: String): String = NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""

/** Called once from AlongsideiOSApp.swift's init - the iOS equivalent of AlongsideApplication.onCreate. */
public fun initKoin() {
    startKoin {
        modules(
            iosAppModule(
                firebaseApiKey = FIREBASE_API_KEY,
                firebaseProjectId = FIREBASE_PROJECT_ID,
                storageBucket = FIREBASE_STORAGE_BUCKET,
                googlePlacesApiKey = infoPlistString("GooglePlacesAPIKey"),
                geminiApiKey = infoPlistString("GeminiAPIKey"),
            ),
            dataModule,
            appModule,
            authFeatureModule,
            onboardingFeatureModule,
            pairingFeatureModule,
            diaryFeatureModule,
            placesFeatureModule,
            matcherFeatureModule,
            settingsFeatureModule,
            recapFeatureModule,
        )
    }
}

/**
 * The iOS host's equivalent of MainActivity. [googleAuthProvider] is constructed in Swift
 * (`GoogleSignInAuthProvider.swift`) since `GoogleAuthProvider` is deliberately callback-based,
 * not `suspend`, so a Swift class can implement it directly against the real GIDSignIn SDK -
 * see [GoogleAuthProvider]'s kdoc. [IosPermissionController], unlike the auth provider, is plain
 * Kotlin (PHPhotoLibrary/UNUserNotificationCenter are system frameworks, directly reachable via
 * cinterop - no Swift needed).
 *
 * [currentIosPendingShareText] is read directly in the Compose body - `AlongsideiOSApp.swift`
 * pushes into it (via [setIosPendingShareText]) on cold start and every foreground, after reading
 * the Share Extension's App Group hand-off. No Swift-facing signature change needed here, same
 * as Android's `pendingShareText`/`onShareTextConsume` pair in `MainActivity`.
 */
@Suppress("FunctionName")
public fun MainViewController(googleAuthProvider: GoogleAuthProvider): UIViewController =
    ComposeUIViewController {
        AlongsideApp(
            googleAuthProvider = googleAuthProvider,
            permissionController = IosPermissionController(),
            sharePlatform = SharePlatform.IOS,
            pendingShareText = currentIosPendingShareText(),
            onShareTextConsume = { setIosPendingShareText(null) },
        )
    }
