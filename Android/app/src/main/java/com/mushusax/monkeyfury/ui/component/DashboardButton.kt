package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DashboardButton(
    modifier: Modifier = Modifier.Companion,
    text: String,
    icon: Painter? = null,
    onClick: () -> Unit = {}
) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    // Decorative: the text alongside already describes the action.
                    Image(painter = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(text = text)
            }
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
