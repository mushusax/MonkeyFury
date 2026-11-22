package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.R
import com.mushusax.monkeyfury.ui.component.DashboardButton
import com.mushusax.monkeyfury.ui.component.Logo
import com.mushusax.monkeyfury.ui.viewmodel.LoginScreenViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginScreenViewModel = viewModel(factory = LoginScreenViewModel.Factory),
    onLogin: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser != null) onLogin()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Logo()

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            DashboardButton(
                text = "Continue with Google",
                icon = painterResource(R.drawable.ic_google_logo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                onClick = {
                    scope.launch {
                        try {
                            val idToken = viewModel.googleAuthClient.getIdToken(context)
                            viewModel.onGoogleIdToken(idToken)
                        } catch (_: GetCredentialCancellationException) {
                            // user closed the picker, nothing to do
                        } catch (e: GetCredentialException) {
                            viewModel.onSignInFailed(e.message ?: "Google sign-in failed")
                        }
                    }
                })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(modifier = Modifier.fillMaxWidth())
}
