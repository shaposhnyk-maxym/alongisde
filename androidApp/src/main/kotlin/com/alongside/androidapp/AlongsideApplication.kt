package com.alongside.androidapp

import android.app.Application
import com.alongside.androidapp.di.androidAppModule
import com.alongside.feature.auth.di.authFeatureModule
import com.alongside.feature.onboarding.di.onboardingFeatureModule
import com.alongside.feature.pairing.di.pairingFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AlongsideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AlongsideApplication)
            modules(
                androidAppModule(this@AlongsideApplication, getString(R.string.google_api_key)),
                authFeatureModule,
                onboardingFeatureModule,
                pairingFeatureModule,
            )
        }
    }
}
