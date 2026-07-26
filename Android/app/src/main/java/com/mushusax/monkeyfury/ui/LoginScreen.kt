package com.mushusax.monkeyfury.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLogin: (username: String) -> Unit = {}) {
    Column(
        modifier = modifier,
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
            onClick = { onLogin(usernameTextFieldState.text.toString()) })
    }
}

@Preview()
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}