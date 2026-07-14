package com.mushusax.monkeyfury

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DashboardButton(modifier: Modifier = Modifier, text: String) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = text)
        }
    }
}

@Preview()
@Composable
fun DashboardButtonPreview() {
    DashboardButton(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp), text = "Placeholder"
    )
}