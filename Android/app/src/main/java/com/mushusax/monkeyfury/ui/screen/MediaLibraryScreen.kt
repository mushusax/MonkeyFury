package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.R
import com.mushusax.monkeyfury.data.Media
import com.mushusax.monkeyfury.data.MediaKind
import com.mushusax.monkeyfury.data.MediaStatus
import com.mushusax.monkeyfury.data.formatProgress
import com.mushusax.monkeyfury.data.hasProgress
import com.mushusax.monkeyfury.data.progressFraction
import com.mushusax.monkeyfury.data.progressLabel
import com.mushusax.monkeyfury.ui.component.ActivityWallpaper
import com.mushusax.monkeyfury.ui.component.ContentPanel
import com.mushusax.monkeyfury.ui.component.LogAmountDialog
import com.mushusax.monkeyfury.ui.component.MediaEditDialog
import com.mushusax.monkeyfury.ui.component.RowAction
import com.mushusax.monkeyfury.ui.viewmodel.MediaLibraryViewModel

/** The kind tabs: All, then each kind. */
private val KIND_TABS: List<MediaKind?> = listOf(null) + MediaKind.entries

@Composable
fun MediaLibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaLibraryViewModel = viewModel(factory = MediaLibraryViewModel.Factory),
    onStatsClicked: () -> Unit = {}
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val kindFilter by viewModel.kindFilter.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var dialogOpen by remember { mutableStateOf(false) }
    // The item being edited, or null when the open dialog is creating one
    var editing by remember { mutableStateOf<Media?>(null) }
    // The item a custom amount is being logged for, from a long press on +1
    var logging by remember { mutableStateOf<Media?>(null) }

    ActivityWallpaper(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Grows only as tall as its content; the bottom margin keeps it clear of the + button
            ContentPanel(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Media",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onStatsClicked) { Text("Stats") }
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    KIND_TABS.forEachIndexed { index, kind ->
                        SegmentedButton(
                            selected = kindFilter == kind,
                            onClick = { viewModel.selectKind(kind) },
                            shape = SegmentedButtonDefaults.itemShape(index, KIND_TABS.size),
                            label = { Text(kind?.pluralLabel ?: "All", maxLines = 1) },
                            icon = {}
                        )
                    }
                }

                if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                error?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(modifier = Modifier.weight(1f), text = message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }

                if (sections.isEmpty() && !loading && error == null) {
                    val what = kindFilter?.pluralLabel?.lowercase() ?: "books, anime or movies"
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "No $what yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                    sections.forEach { (status, items) ->
                        item(key = "header-$status") {
                            Text(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                text = "${status.label} (${items.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(items = items, key = { it.publicId }) { media ->
                            MediaRow(
                                media = media,
                                showKind = kindFilter == null,
                                enabled = !loading,
                                onClick = {
                                    editing = media
                                    dialogOpen = true
                                },
                                onAddOne = { viewModel.logProgress(media.publicId, 1) },
                                onAddMany = { logging = media },
                                onWatched = { viewModel.markWatched(media) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            onClick = {
                editing = null
                dialogOpen = true
            }
        ) {
            Icon(painter = painterResource(id = R.drawable.outline_add), contentDescription = "Add to library")
        }
    }

    if (dialogOpen) {
        val media = editing
        MediaEditDialog(
            media = media,
            initialKind = kindFilter ?: MediaKind.BOOK,
            onDismiss = { dialogOpen = false },
            onSave = { params -> viewModel.save(media?.publicId, params, onDone = { dialogOpen = false }) },
            onArchive = media?.let { { viewModel.archive(it.publicId, onDone = { dialogOpen = false }) } },
            saving = loading
        )
    }

    logging?.let { media ->
        LogAmountDialog(
            media = media,
            onDismiss = { logging = null },
            onLog = { amount -> viewModel.logProgress(media.publicId, amount, onDone = { logging = null }) },
            saving = loading
        )
    }
}

@Composable
private fun MediaRow(
    media: Media,
    showKind: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onAddOne: () -> Unit,
    onAddMany: () -> Unit,
    onWatched: () -> Unit
) {
    val detail = listOfNotNull(
        media.kind.label.takeIf { showKind },
        media.creator.takeIf { it.isNotEmpty() },
        media.formatProgress().takeIf { it.isNotEmpty() && media.status != MediaStatus.COMPLETED }
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = media.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                media.thumbsUp?.let { up ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (up) "👍" else "👎",
                        modifier = Modifier.semantics { contentDescription = if (up) "Thumbs up" else "Thumbs down" }
                    )
                }
            }
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val fraction = media.progressFraction
            if (fraction != null && media.status == MediaStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }

        val canLog = media.kind.hasProgress && media.status != MediaStatus.COMPLETED && media.status != MediaStatus.DROPPED
        val canWatch = !media.kind.hasProgress && media.status != MediaStatus.COMPLETED
        if (canLog || canWatch) {
            Spacer(modifier = Modifier.width(12.dp))
            RowAction(
                text = if (canLog) "+1" else "Watched",
                description = if (canLog) "Add one ${media.kind.progressLabel.lowercase().removeSuffix("s")}" else "Mark watched",
                enabled = enabled,
                onClick = if (canLog) onAddOne else onWatched,
                onLongClick = if (canLog) onAddMany else null
            )
        }
    }
}
