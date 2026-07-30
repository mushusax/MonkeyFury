package com.mushusax.monkeyfury.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.mushusax.monkeyfury.R

@Composable
fun Logo(modifier: Modifier = Modifier.Companion) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = "Monkey Fury Logo",
        modifier = modifier
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun PreviewLogo() {
    Logo(modifier = Modifier.Companion)
}