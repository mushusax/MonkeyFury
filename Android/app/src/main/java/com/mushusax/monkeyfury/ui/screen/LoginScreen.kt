package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import com.mushusax.monkeyfury.ui.component.LoginButton
import com.mushusax.monkeyfury.ui.component.Logo
import com.mushusax.monkeyfury.ui.viewmodel.LoginScreenViewModel
import com.mushusax.monkeyfury.ui.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginScreenViewModel = viewModel(),
    onLogin: (MonkeyFuryApi.User) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.NavigateToDashboard -> {
                onLogin((uiState as LoginUiState.NavigateToDashboard).user)
                viewModel.uiState.value = LoginUiState.Idle
            }

            else -> {}
        }
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
            val usernameTextFieldState = rememberTextFieldState()
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(1f),
                state = usernameTextFieldState,
                label = {
                    Text(text = "username")
                })
            LoginButton(
                text = "Login",
                modifier = Modifier.fillMaxWidth(1f),
                onClick = {
                    viewModel.logIn(usernameTextFieldState.text.toString())
                })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(modifier = Modifier.fillMaxWidth())
}