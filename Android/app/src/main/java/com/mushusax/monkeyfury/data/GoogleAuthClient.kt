package com.mushusax.monkeyfury.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(private val appContext: Context) {

    // Must match auth.google.webClientId in the backend's application.yaml
    private val webClientId = "399364014623-3uqeqnouqg5f2iq4l4aucee02aksf7tm.apps.googleusercontent.com"

    /** Shows Google's account picker and returns a Google ID token. Needs an Activity context. */
    suspend fun getIdToken(activityContext: Context): String {
        val credentialManager = CredentialManager.create(activityContext)
        val credential = try {
            credentialManager.getCredential(activityContext, request(authorizedOnly = true)).credential
        } catch (_: NoCredentialException) {
            credentialManager.getCredential(activityContext, request(authorizedOnly = false)).credential
        }

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unexpected credential type: ${credential.type}")
    }

    suspend fun clearCredentialState() {
        CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
    }

    private fun request(authorizedOnly: Boolean) = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(authorizedOnly)
                .setAutoSelectEnabled(authorizedOnly)
                .build()
        )
        .build()
}
