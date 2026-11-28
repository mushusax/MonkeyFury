package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mushusax.monkeyfury.data.Media
import com.mushusax.monkeyfury.data.MediaKind
import com.mushusax.monkeyfury.data.MediaParams
import com.mushusax.monkeyfury.data.MediaStatus
import com.mushusax.monkeyfury.data.creatorLabel
import com.mushusax.monkeyfury.data.hasProgress
import com.mushusax.monkeyfury.data.progressLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MAX_TITLE = 200
private const val MAX_NOTES = 5000
private const val MAX_TOTAL = 100_000

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** The rating choices, in the order shown. Null is not rated yet. */
private val RATINGS: List<Pair<Boolean?, String>> = listOf(null to "—", true to "👍", false to "👎")

/**
 * Creates an item when [media] is null, otherwise edits it. New items start as [initialKind].
 * Start and finish dates fill in from the status: leaving Planned starts it today, and Completed
 * finishes it today. [onArchive] shows a Remove button when set.
 */
@Composable
fun MediaEditDialog(
    media: Media?,
    initialKind: MediaKind,
    onDismiss: () -> Unit,
    onSave: (MediaParams) -> Unit,
    onArchive: (() -> Unit)? = null,
    saving: Boolean = false
) {
    var kind by remember { mutableStateOf(media?.kind ?: initialKind) }
    var title by remember { mutableStateOf(media?.title ?: "") }
    var creator by remember { mutableStateOf(media?.creator ?: "") }
    var status by remember { mutableStateOf(media?.status ?: MediaStatus.PLANNED) }
    var progress by remember { mutableStateOf(media?.progress?.takeIf { it > 0 }?.toString() ?: "") }
    var total by remember { mutableStateOf(media?.total?.toString() ?: "") }
    var thumbsUp by remember { mutableStateOf(media?.thumbsUp) }
    var notes by remember { mutableStateOf(media?.notes ?: "") }

    val parsedTotal = total.trim().toIntOrNull()
    val totalError = total.isNotBlank() && (parsedTotal == null || parsedTotal !in 1..MAX_TOTAL)
    val parsedProgress = if (progress.isBlank()) 0 else progress.trim().toIntOrNull()
    val progressError = parsedProgress == null || (parsedTotal != null && parsedProgress > parsedTotal)
    val canSave = title.isNotBlank() && (!kind.hasProgress || (!totalError && !progressError)) && !saving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (media == null) "New ${kind.label.lowercase()}" else "Edit ${kind.label.lowercase()}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    MediaKind.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = kind == option,
                            onClick = { kind = option },
                            shape = SegmentedButtonDefaults.itemShape(index, MediaKind.entries.size),
                            label = { Text(option.label) }
                        )
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    onValueChange = { title = it.take(MAX_TITLE) },
                    label = { Text("Title") },
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = creator,
                    onValueChange = { creator = it.take(MAX_TITLE) },
                    label = { Text("${kind.creatorLabel} (optional)") },
                    singleLine = true
                )

                Spinner(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Status",
                    options = MediaStatus.entries.map { it.label },
                    selectedOption = status.label,
                    onOptionSelected = { label ->
                        val chosen = MediaStatus.entries.first { it.label == label }
                        // Most things finished are liked, so completing starts at thumbs up
                        if (chosen == MediaStatus.COMPLETED && thumbsUp == null) thumbsUp = true
                        status = chosen
                    }
                )

                if (kind.hasProgress) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = progress,
                            onValueChange = { progress = it.filter(Char::isDigit).take(6) },
                            label = { Text(kind.progressLabel) },
                            placeholder = { Text("0") },
                            isError = progressError,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = total,
                            onValueChange = { total = it.filter(Char::isDigit).take(6) },
                            label = { Text("Total") },
                            isError = totalError,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    if (progressError || totalError) {
                        Text(
                            text = if (totalError) "Total must be between 1 and $MAX_TOTAL" else "Can't be past the total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text("Rating", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RATINGS.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = thumbsUp == value,
                            onClick = { thumbsUp = value },
                            shape = SegmentedButtonDefaults.itemShape(index, RATINGS.size),
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = notes,
                    onValueChange = { notes = it.take(MAX_NOTES) },
                    label = { Text("Notes (optional)") },
                    minLines = 2
                )

                val dates = listOfNotNull(
                    media?.startedOn?.let { "Started ${LocalDate.parse(it).format(DATE_FORMAT)}" },
                    media?.finishedOn?.let { "Finished ${LocalDate.parse(it).format(DATE_FORMAT)}" }
                )
                if (dates.isNotEmpty()) {
                    Text(
                        text = dates.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onArchive != null) {
                    TextButton(onClick = onArchive, enabled = !saving) {
                        Text("Remove ${kind.label.lowercase()}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val today = LocalDate.now().toString()
                    val finishedOn = if (status == MediaStatus.COMPLETED) media?.finishedOn ?: today else null
                    // A movie is just watched, so only books and anime get a start day
                    val startedOn = when {
                        !kind.hasProgress -> null
                        status == MediaStatus.PLANNED -> media?.startedOn
                        else -> media?.startedOn ?: finishedOn ?: today
                    }
                    onSave(
                        MediaParams(
                            kind = kind,
                            title = title.trim(),
                            creator = creator.trim(),
                            status = status,
                            progress = if (kind.hasProgress) parsedProgress ?: 0 else 0,
                            total = if (kind.hasProgress) parsedTotal else null,
                            thumbsUp = thumbsUp,
                            startedOn = startedOn,
                            finishedOn = finishedOn,
                            notes = notes.trim()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Asks how many pages or episodes to log for [media]; a negative number undoes. */
@Composable
fun LogAmountDialog(
    media: Media,
    onDismiss: () -> Unit,
    onLog: (Int) -> Unit,
    saving: Boolean = false
) {
    var amount by remember { mutableStateOf("") }
    val parsed = amount.trim().toIntOrNull()?.takeIf { it != 0 && it in -MAX_TOTAL..MAX_TOTAL }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${media.kind.progressLabel.lowercase()}") },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amount,
                onValueChange = { text -> amount = text.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(7) },
                label = { Text(media.title) },
                supportingText = { Text("Use a negative number to undo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(enabled = parsed != null && !saving, onClick = { parsed?.let(onLog) }) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview
@Composable
fun PreviewMediaEditDialog() {
    MediaEditDialog(media = null, initialKind = MediaKind.BOOK, onDismiss = {}, onSave = {})
}
