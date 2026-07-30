package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.mushusax.monkeyfury.R

@Composable
fun DropdownMenu(
    modifier: Modifier = Modifier.Companion,
    expanded: Boolean = false,
    onLogout: () -> Unit = {},
    onHealthCheck: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(expanded) }
    Box(
        modifier = modifier
            .wrapContentHeight()
            .wrapContentWidth(),
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_more_vert),
                contentDescription = "More options",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Log out") },
                onClick = {
                    onLogout()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Health Check") },
                onClick = {
                    onHealthCheck()
                    expanded = false
                }
            )
        }
    }
}

@Preview()
@Composable
fun PreviewDropdownMenu() {
    DropdownMenu(modifier = Modifier.Companion, expanded = true)
}