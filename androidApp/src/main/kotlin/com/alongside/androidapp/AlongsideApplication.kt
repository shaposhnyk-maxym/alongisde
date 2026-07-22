package com.alongside.androidapp

import android.app.Application
import com.alongside.androidapp.di.androidAppModule
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.ui.component.installAlongsideImageLoader
import com.alongside.data.di.dataModule
import com.alongside.feature.auth.di.authFeatureModule
import com.alongside.feature.diary.di.diaryFeatureModule
import com.alongside.feature.matcher.di.matcherFeatureModule
import com.alongside.feature.onboarding.di.onboardingFeatureModule
import com.alongside.feature.pairing.di.pairingFeatureModule
import com.alongside.feature.places.di.placesFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

class AlongsideApplication :
    Application(),
    KoinComponent {
    override fun onCreate() {
        super.onCreate()
        installAlongsideImageLoader(this)
        startKoin {
            androidContext(this@AlongsideApplication)
            modules(
                androidAppModule(
                    this@AlongsideApplication,
                    getString(R.string.google_api_key),
                    getString(R.string.project_id),
                    getString(R.string.google_storage_bucket),
                    BuildConfig.GOOGLE_PLACES_API_KEY,
                    BuildConfig.GEMINI_API_KEY,
                ),
                dataModule,
                authFeatureModule,
                onboardingFeatureModule,
                pairingFeatureModule,
                diaryFeatureModule,
                placesFeatureModule,
                matcherFeatureModule,
            )
        }
        // Idempotent (ExistingPeriodicWorkPolicy.KEEP) - safe to call on every process start,
        // docs/roadmap.md M12.11's backstop for missed event-driven scheduleOneOff enqueues.
        get<BackgroundWorkScheduler>().ensurePeriodicSweepScheduled()
    }
}
