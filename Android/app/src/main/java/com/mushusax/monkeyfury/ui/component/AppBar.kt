package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AppBar(modifier: Modifier = Modifier.Companion, onLogout: () -> Unit = {}) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DropdownMenu(modifier = Modifier.Companion, onLogout = onLogout)
        }
    }
}

@Preview()
@Composable
fun PreviewAppBar() {
    AppBar(modifier = Modifier.fillMaxWidth(1f))
}