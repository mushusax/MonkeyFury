package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.ui.component.ActivityWallpaper
import com.mushusax.monkeyfury.ui.component.DashboardButton
import com.mushusax.monkeyfury.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
    onLogout: () -> Unit = {},
    onActivityTrackingClicked: () -> Unit = {},
    onFlashcardClicked: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    LaunchedEffect(currentUser) {
        if (currentUser == null) onLogout()
    }

    ActivityWallpaper(modifier = modifier) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
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
                        .padding(PaddingValues(16.dp)), text = "Activity Tracking",
                    onClick = onActivityTrackingClicked
                )
            }
            item {
                DashboardButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .aspectRatio(1f)
                        .padding(PaddingValues(16.dp)), text = "Flashcard",
                    onClick = onFlashcardClicked
                )
            }
            item {
                DashboardButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .aspectRatio(1f)
                        .padding(PaddingValues(16.dp)), text = "Logout",
                    // Navigation back to Login happens in the LaunchedEffect above, once the user is cleared
                    onClick = { viewModel.logOut() }
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