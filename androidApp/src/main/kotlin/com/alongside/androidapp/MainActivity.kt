package com.alongside.androidapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.alongside.app.AlongsideApp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.auth.CredentialManagerGoogleAuthProvider
import com.alongside.feature.onboarding.AndroidPermissionController

class MainActivity : ComponentActivity() {
    // Not `remember`ed inside setContent - onNewIntent (a plain Activity callback, not
    // composable) needs to be able to update this from outside composition.
    private var pendingShareText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShareText = extractShareText(intent)
        // The two Activity-bound seams the auth gate needs; everything else the graph
        // resolves through Koin.
        val googleAuthProvider = CredentialManagerGoogleAuthProvider(this, getString(R.string.default_web_client_id))
        val permissionController = AndroidPermissionController(this)
        setContent {
            AlongsideTheme {
                AlongsideApp(
                    googleAuthProvider = googleAuthProvider,
                    permissionController = permissionController,
                    pendingShareText = pendingShareText,
                    onShareTextConsume = { pendingShareText = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // `singleTask` launch mode (AndroidManifest.xml) routes a second ACTION_SEND here
        // instead of spinning up a new instance - onCreate alone would miss this share.
        extractShareText(intent)?.let { pendingShareText = it }
    }

    private fun extractShareText(intent: Intent): String? =
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }
}
