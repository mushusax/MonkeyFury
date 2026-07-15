package com.mushusax.monkeyfury

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLogin: () -> Unit = {}) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DashboardButton(
            text = "Login",
            modifier = Modifier
                .size(120.dp)
                .aspectRatio(1f),
            onClick = onLogin
        )
    }
}

@Preview()
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}