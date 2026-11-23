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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mushusax.monkeyfury.data.Note
import com.mushusax.monkeyfury.data.NoteParams

/** Creates a card when [note] is null, otherwise edits it. [onDelete] shows a Delete button when set. */
@Composable
fun NoteEditDialog(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (NoteParams) -> Unit,
    onDelete: (() -> Unit)? = null,
    saving: Boolean = false
) {
    var front by remember { mutableStateOf(note?.front ?: "") }
    var back by remember { mutableStateOf(note?.back ?: "") }
    var reversible by remember { mutableStateOf(note?.reversible ?: false) }

    val canSave = front.isNotBlank() && back.isNotBlank() && !saving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "New card" else "Edit card") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Both directions", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Reversible",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = reversible, onCheckedChange = { reversible = it })
                }

                if (onDelete != null) {
                    TextButton(onClick = onDelete, enabled = !saving) {
                        Text("Delete card", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(NoteParams(front.trim(), back.trim(), reversible)) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview
@Composable
fun PreviewNoteEditDialog() {
    NoteEditDialog(note = null, onDismiss = {}, onSave = {})
}
