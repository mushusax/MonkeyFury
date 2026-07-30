package com.mushusax.monkeyfury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onTasks: () -> Unit = {}
) {
    Column(modifier = modifier) {
        AppBar(
            modifier = Modifier
                .fillMaxWidth(1f)
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding(),
            onLogout = onLogout
        )

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(1f),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            item {
                DashboardButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .aspectRatio(1f)
                        .padding(PaddingValues(16.dp)), text = "Tasks",
                    onClick = onTasks
                )
            }
            item {
                DashboardButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .aspectRatio(1f)
                        .padding(PaddingValues(16.dp)), text = "Users"
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(modifier = Modifier.fillMaxSize())
}