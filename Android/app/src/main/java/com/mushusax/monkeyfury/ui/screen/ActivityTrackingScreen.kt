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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mushusax.monkeyfury.ui.component.ActivityWallpaper
import com.mushusax.monkeyfury.ui.component.DashboardButton

/** The Activity Tracking hub: one DashboardButton per part of the feature, over the wallpaper. */
@Composable
fun ActivityTrackingScreen(
    modifier: Modifier = Modifier,
    onDailyReviewClicked: () -> Unit = {},
    onCalendarClicked: () -> Unit = {},
    onActivitiesClicked: () -> Unit = {}
) {
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
            item { HubButton(text = "Daily Review", onClick = onDailyReviewClicked) }
            item { HubButton(text = "Calendar", onClick = onCalendarClicked) }
            item { HubButton(text = "Activities", onClick = onActivitiesClicked) }
        }
    }
}

/** Same size and spacing as the Dashboard's buttons. */
@Composable
private fun HubButton(text: String, onClick: () -> Unit) {
    DashboardButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .aspectRatio(1f)
            .padding(PaddingValues(16.dp)),
        text = text,
        onClick = onClick
    )
}

@Preview(showSystemUi = true)
@Composable
fun ActivityTrackingScreenPreview() {
    ActivityTrackingScreen(modifier = Modifier.fillMaxSize())
}
