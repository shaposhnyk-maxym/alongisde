package com.alongside.feature.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val TAG = "GoogleAuthProvider"

/**
 * Android's real [GoogleAuthProvider]: Credential Manager + Google Identity Services. The native
 * sign-in UI flow this drives is a system dialog and is deliberately not unit-tested (M5 accept
 * criteria) - only AuthContainer's handling of the resulting [GoogleSignInResult] is.
 */
public class CredentialManagerGoogleAuthProvider(
    private val activity: Activity,
    private val serverClientId: String,
) : GoogleAuthProvider {
    private val credentialManager = CredentialManager.create(activity)
    private val scope = MainScope()

    override fun signIn(onResult: (GoogleSignInResult) -> Unit) {
        requestCredential(filterByAuthorizedAccounts = false, onResult)
    }

    override fun signInSilently(onResult: (GoogleSignInResult) -> Unit) {
        requestCredential(filterByAuthorizedAccounts = true, onResult)
    }

    private fun requestCredential(
        filterByAuthorizedAccounts: Boolean,
        onResult: (GoogleSignInResult) -> Unit,
    ) {
        Log.d(TAG, "signIn: requesting credential (filterByAuthorizedAccounts=$filterByAuthorizedAccounts)")
        scope.launch {
            val request =
                GetCredentialRequest
                    .Builder()
                    .addCredentialOption(
                        GetGoogleIdOption
                            .Builder()
                            .setServerClientId(serverClientId)
                            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                            .build(),
                    ).build()
            onResult(awaitCredential(request))
        }
    }

    // Catching broadly here is deliberate: GoogleIdTokenCredential.createFrom throws its own
    // GoogleIdTokenParsingException (not a GetCredentialException subtype), and any exception
    // that escapes this function is otherwise swallowed by the enclosing launch{} with no visible
    // symptom - logging every branch is the point, not an oversight.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun awaitCredential(request: GetCredentialRequest): GoogleSignInResult =
        try {
            val response = credentialManager.getCredential(activity, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Log.d(TAG, "signIn: success (subject=${credential.id})")
            GoogleSignInResult.Success(credential.idToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "signIn: cancelled by user", e)
            GoogleSignInResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "signIn: failed (${e::class.simpleName}): ${e.message}", e)
            GoogleSignInResult.Failure(e.message ?: e::class.simpleName)
        }
}
