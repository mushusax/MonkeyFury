package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A small outlined button on the black panel, like +1 on a media row; [onLongClick] opens a custom amount. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowAction(
    text: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black,
        contentColor = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.8f else 0.3f))
    ) {
        Text(
            modifier = Modifier
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = onLongClick?.let { "Log a different amount" }
                )
                .semantics { contentDescription = description }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
