package com.alongside.app

import androidx.compose.ui.window.ComposeUIViewController
import com.alongside.app.di.appModule
import com.alongside.app.di.iosAppModule
import com.alongside.data.di.dataModule
import com.alongside.feature.auth.GoogleAuthProvider
import com.alongside.feature.auth.di.authFeatureModule
import com.alongside.feature.diary.di.diaryFeatureModule
import com.alongside.feature.matcher.di.matcherFeatureModule
import com.alongside.feature.onboarding.StubPermissionController
import com.alongside.feature.onboarding.di.onboardingFeatureModule
import com.alongside.feature.pairing.di.pairingFeatureModule
import com.alongside.feature.places.di.placesFeatureModule
import com.alongside.feature.recap.di.recapFeatureModule
import com.alongside.feature.settings.di.settingsFeatureModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

/**
 * Same Firebase project as `androidApp/google-services.json` - see [iosAppModule]'s kdoc for why
 * these are plain constants here instead of a parsed config file.
 */
private const val FIREBASE_API_KEY = "AIzaSyBngsPMdA2piAYsJNTTAzVjuA5hbzfX9HY"
private const val FIREBASE_PROJECT_ID = "alongside-b05f2"

/** Called once from AlongsideiOSApp.swift's init - the iOS equivalent of AlongsideApplication.onCreate. */
public fun initKoin() {
    startKoin {
        modules(
            iosAppModule(FIREBASE_API_KEY, FIREBASE_PROJECT_ID),
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
 * see [GoogleAuthProvider]'s kdoc. [StubPermissionController] is still temporary until M7 lands
 * real iOS permissions.
 */
public fun MainViewController(googleAuthProvider: GoogleAuthProvider): UIViewController =
    ComposeUIViewController {
        AlongsideApp(
            googleAuthProvider = googleAuthProvider,
            permissionController = StubPermissionController(),
        )
    }
