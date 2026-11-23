package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.R
import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityKind
import com.mushusax.monkeyfury.data.formatValue
import com.mushusax.monkeyfury.ui.component.ActivityEditDialog
import com.mushusax.monkeyfury.ui.viewmodel.ActivityListViewModel
import com.mushusax.monkeyfury.ui.component.ActivityWallpaper
import com.mushusax.monkeyfury.ui.component.ContentPanel

/** Shown for activities with no category. */
internal const val NO_CATEGORY = "Other"

/** Activities grouped by category, alphabetical, with uncategorized ones last. */
internal fun groupByCategory(activities: List<Activity>): List<Pair<String, List<Activity>>> =
    activities.groupBy { it.category }
        .toList()
        .sortedWith(compareBy({ it.first.isEmpty() }, { it.first.lowercase() }))
        .map { (category, items) -> category.ifEmpty { NO_CATEGORY } to items }

@Composable
fun ActivityListScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityListViewModel = viewModel(factory = ActivityListViewModel.Factory)
) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var dialogOpen by remember { mutableStateOf(false) }
    // The activity being edited, or null when the open dialog is creating one
    var editing by remember { mutableStateOf<Activity?>(null) }

    ActivityWallpaper(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // The panel only grows as tall as its content, so short lists leave the bottom of the
            // wallpaper showing; its bottom margin keeps it clear of the + button.
            ContentPanel(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
            ) {
                if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                error?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }

                if (activities.isEmpty() && !loading && error == null) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "No activities yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                    groupByCategory(activities).forEach { (category, items) ->
                        item(key = "header-$category") {
                            Text(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(items = items, key = { it.publicId }) { activity ->
                            ActivityRow(activity = activity, onClick = {
                                editing = activity
                                dialogOpen = true
                            })
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
            Icon(painter = painterResource(id = R.drawable.outline_add), contentDescription = "Add activity")
        }
    }

    if (dialogOpen) {
        val activity = editing
        ActivityEditDialog(
            activity = activity,
            categories = activities.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted(),
            onDismiss = { dialogOpen = false },
            onSave = { params -> viewModel.save(activity?.publicId, params, onDone = { dialogOpen = false }) },
            onArchive = activity?.let { { viewModel.archive(it.publicId, onDone = { dialogOpen = false }) } },
            saving = loading
        )
    }
}

@Composable
private fun ActivityRow(activity: Activity, onClick: () -> Unit) {
    val detail = buildList {
        add(activity.kind.label)
        if (activity.kind == ActivityKind.NUMBER && activity.unit.isNotEmpty()) add(activity.unit)
        if (activity.lowerIsBetter) add("lower is better")
        activity.goal?.let { add("${if (activity.lowerIsBetter) "limit" else "goal"} ${activity.formatValue(it)}") }
    }.joinToString(" · ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = activity.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
