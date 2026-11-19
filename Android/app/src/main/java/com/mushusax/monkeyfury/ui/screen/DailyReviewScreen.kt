package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityKind
import com.mushusax.monkeyfury.ui.viewmodel.DailyReviewViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
fun DailyReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: DailyReviewViewModel = viewModel(factory = DailyReviewViewModel.Factory)
) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val inputs by viewModel.inputs.collectAsStateWithLifecycle()
    // Collected so rows and the Save button recompose when saved values change
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val rowErrors by viewModel.rowErrors.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    // Set when the user tries to leave a day that has unsaved edits
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    val dirty = activities.any { (inputs[it.publicId] ?: "") != (saved[it.publicId] ?: "") }
    val today = LocalDate.now()

    fun goTo(target: LocalDate) {
        if (dirty) pendingDate = target else viewModel.setDate(target)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.semantics { contentDescription = "Previous day" },
                onClick = { goTo(date.minusDays(1)) }
            ) { Text("◀") }
            Text(
                modifier = Modifier.weight(1f),
                text = if (date == today) "Today, ${date.format(DATE_FORMATTER)}" else date.format(DATE_FORMATTER),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            IconButton(
                modifier = Modifier.semantics { contentDescription = "Next day" },
                enabled = date.isBefore(today),
                onClick = { goTo(date.plusDays(1)) }
            ) { Text("▶") }
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(modifier = Modifier.weight(1f), text = it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load() }) { Text("Retry") }
            }
        }

        if (activities.isEmpty() && !loading && error == null) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "No activities yet. Add some from Activity Tracking.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
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
                    ReviewRow(
                        activity = activity,
                        input = inputs[activity.publicId] ?: "",
                        error = rowErrors[activity.publicId],
                        enabled = !loading,
                        onInput = { viewModel.setInput(activity.publicId, it) }
                    )
                    HorizontalDivider()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = if (dirty) "Unsaved changes" else message ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(enabled = dirty && !loading, onClick = { viewModel.save() }) { Text("Save") }
        }
    }

    pendingDate?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDate = null },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved values for ${date.format(DATE_FORMATTER)}.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDate = null
                    viewModel.setDate(target)
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDate = null }) { Text("Keep editing") }
            }
        )
    }
}

@Composable
private fun ReviewRow(
    activity: Activity,
    input: String,
    error: String?,
    enabled: Boolean,
    onInput: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.weight(1f), text = activity.name, style = MaterialTheme.typography.bodyLarge)
        when (activity.kind) {
            ActivityKind.BOOLEAN -> Checkbox(
                checked = input == "1",
                enabled = enabled,
                onCheckedChange = { onInput(if (it) "1" else "0") }
            )

            ActivityKind.NUMBER, ActivityKind.DURATION -> {
                val isDuration = activity.kind == ActivityKind.DURATION
                OutlinedTextField(
                    modifier = Modifier.width(140.dp),
                    value = input,
                    onValueChange = onInput,
                    enabled = enabled,
                    singleLine = true,
                    placeholder = { Text(if (isDuration) "h:mm" else "0") },
                    suffix = if (!isDuration && activity.unit.isNotEmpty()) {
                        { Text(activity.unit) }
                    } else null,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        // Decimal keyboards often lack ":", so durations get the full keyboard
                        keyboardType = if (isDuration) KeyboardType.Text else KeyboardType.Decimal
                    )
                )
            }
        }
    }
}
