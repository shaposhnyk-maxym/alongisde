package com.alongside.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.alongside.app.AlongsideApp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.auth.CredentialManagerGoogleAuthProvider
import com.alongside.feature.onboarding.AndroidPermissionController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The two Activity-bound seams the auth gate needs; everything else the graph
        // resolves through Koin.
        val googleAuthProvider = CredentialManagerGoogleAuthProvider(this, getString(R.string.default_web_client_id))
        val permissionController = AndroidPermissionController(this)
        setContent {
            AlongsideTheme {
                AlongsideApp(
                    googleAuthProvider = googleAuthProvider,
                    permissionController = permissionController,
                )
            }
        }
    }
}
