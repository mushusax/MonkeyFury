package com.mushusax.monkeyfury.ui.component

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun DashboardButton(
    modifier: Modifier = Modifier.Companion,
    text: String,
    icon: Painter? = null,
    /** Raw Lottie JSON resource shown in place of [text]; [text] is kept as the accessibility label. */
    @RawRes animation: Int? = null,
    /** A count in the top right corner, e.g. cards due; hidden when null or zero. */
    badge: Int? = null,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Black, contentColor = Color.White),
        onClick = onClick
    ) {
        // Box so the badge can sit over the content
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (animation != null) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animation))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = text }
                    )
                } else {
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
            if (badge != null && badge > 0) {
                Text(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    text = if (badge > 99) "99+" else badge.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
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
