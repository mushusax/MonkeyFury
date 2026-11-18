package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.ui.viewmodel.GraphScreenViewModel

@Composable
fun GraphScreen(
    modifier: Modifier = Modifier,
    viewModel: GraphScreenViewModel = viewModel()
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = "graph")
    }
}

@Preview
@Composable
fun PreviewGraphScreen() {
    GraphScreen(modifier = Modifier.fillMaxSize())
}
