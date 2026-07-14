package com.mushusax.monkeyfury

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        modifier = modifier,
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
                    .padding(PaddingValues(16.dp)),
                text = "1"
            )
        }
        item {
            DashboardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .aspectRatio(1f)
                    .padding(PaddingValues(16.dp)), text = "2"
            )
        }
        item {
            DashboardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .aspectRatio(1f)
                    .padding(PaddingValues(16.dp)), text = "3"
            )
        }
        item {
            DashboardButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .aspectRatio(1f)
                    .padding(PaddingValues(16.dp)), text = "4"
            )
        }
    }
}

@Preview()
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(modifier = Modifier.fillMaxSize())
}