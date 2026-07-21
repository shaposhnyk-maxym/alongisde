package com.alongside.androidapp

import android.app.Application
import android.content.Intent
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Roadmap M13.2 accept criterion: the app's manifest actually declares a reachable
 * `ACTION_SEND`/`text/plain` entry point (Google Maps' "Share" sheet), not just that
 * `MainActivity` exists. Only merged-manifest `<activity>`/`<intent-filter>` resolution matters
 * here, independent of which `Application` Robolectric instantiates - a plain stub Application
 * (docs/roadmap.md M12.11) avoids booting the real `AlongsideApplication.onCreate()`, which now
 * touches `WorkManager.getInstance(...)`, unavailable this early in a bare Robolectric environment.
 */
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class ShareIntentManifestTest {
    @Test
    fun `ACTION_SEND with text-plain resolves to MainActivity`() {
        val context = RuntimeEnvironment.getApplication()
        val shareIntent =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .addCategory(Intent.CATEGORY_DEFAULT)

        val resolveInfos = context.packageManager.queryIntentActivities(shareIntent, 0)

        assertTrue(resolveInfos.any { it.activityInfo.name == MainActivity::class.java.name })
    }
}
