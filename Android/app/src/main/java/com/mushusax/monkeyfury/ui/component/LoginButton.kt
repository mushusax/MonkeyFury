package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LoginButton(modifier: Modifier = Modifier.Companion, text: String, onClick: () -> Unit = {}) {
    ElevatedButton(modifier = modifier, onClick = onClick) {
        Text(text = text)
    }
}

@Preview()
@Composable
fun LoginButtonPreview() {
    LoginButton(modifier = Modifier.fillMaxWidth(1f), text = "Placeholder", {})
}