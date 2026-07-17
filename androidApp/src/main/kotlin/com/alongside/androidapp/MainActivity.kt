package com.alongside.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.auth.CredentialManagerGoogleAuthProvider
import com.alongside.feature.auth.presentation.AuthContainer
import com.alongside.feature.auth.presentation.AuthScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Shows the M5 auth screen directly - Navigation 3 (AlongsideApp's real graph) lands in M6+.
        val googleAuthProvider = CredentialManagerGoogleAuthProvider(this, getString(R.string.default_web_client_id))
        setContent {
            AlongsideTheme {
                val container = koinViewModel<AuthContainer> { parametersOf(googleAuthProvider) }
                AuthScreen(container)
            }
        }
    }
}
