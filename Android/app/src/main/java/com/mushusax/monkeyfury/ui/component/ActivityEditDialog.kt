package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityKind
import com.mushusax.monkeyfury.data.ActivityParams
import com.mushusax.monkeyfury.data.formatDuration
import com.mushusax.monkeyfury.data.formatNumber
import com.mushusax.monkeyfury.data.parseDuration

/**
 * Creates an activity when [activity] is null, otherwise edits it. [categories] are offered as
 * suggestions, but any new category can be typed. [onArchive] shows an Archive button when set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityEditDialog(
    activity: Activity?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (ActivityParams) -> Unit,
    onArchive: (() -> Unit)? = null,
    saving: Boolean = false
) {
    var name by remember { mutableStateOf(activity?.name ?: "") }
    var category by remember { mutableStateOf(activity?.category ?: "") }
    var kind by remember { mutableStateOf(activity?.kind ?: ActivityKind.NUMBER) }
    var unit by remember { mutableStateOf(activity?.unit ?: "") }
    var goal by remember {
        mutableStateOf(
            activity?.goal?.let { if (activity.kind == ActivityKind.DURATION) formatDuration(it) else formatNumber(it) }
                ?: ""
        )
    }
    var lowerIsBetter by remember { mutableStateOf(activity?.lowerIsBetter ?: false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Goal is optional; when it's filled in it must parse for the chosen kind
    val parsedGoal: Double? = when (kind) {
        ActivityKind.BOOLEAN -> null
        ActivityKind.DURATION -> parseDuration(goal)
        ActivityKind.NUMBER -> goal.trim().toDoubleOrNull()
    }
    val goalError = kind != ActivityKind.BOOLEAN && goal.isNotBlank() && (parsedGoal == null || parsedGoal <= 0)
    val canSave = name.isNotBlank() && !goalError && !saving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (activity == null) "New activity" else "Edit activity") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it.take(100) },
                    label = { Text("Name") },
                    singleLine = true
                )

                val suggestions = categories.filter { it.isNotEmpty() && it.contains(category.trim(), ignoreCase = true) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded && suggestions.isNotEmpty(),
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        value = category,
                        onValueChange = {
                            category = it.take(100)
                            categoryExpanded = true
                        },
                        label = { Text("Category") },
                        singleLine = true,
                        trailingIcon = {
                            if (categories.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            }
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded && suggestions.isNotEmpty(),
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        suggestions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Text("Type", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ActivityKind.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = kind == option,
                            onClick = {
                                // The goal's format differs per kind, so start it fresh
                                if (kind != option) goal = ""
                                kind = option
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ActivityKind.entries.size),
                            label = { Text(option.label) }
                        )
                    }
                }

                if (kind == ActivityKind.NUMBER) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = unit,
                        onValueChange = { unit = it.take(20) },
                        label = { Text("Unit (optional)") },
                        placeholder = { Text("mi, drinks, V") },
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lower is better", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (kind == ActivityKind.BOOLEAN) "\"No\" days show full color"
                            else "Days at 0 show full color, like drinks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = lowerIsBetter, onCheckedChange = { lowerIsBetter = it })
                }

                if (kind != ActivityKind.BOOLEAN) {
                    val isDuration = kind == ActivityKind.DURATION
                    val goalWord = if (lowerIsBetter) "limit" else "goal"
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text(if (isDuration) "Daily $goalWord, h:mm (optional)" else "Daily $goalWord (optional)") },
                        placeholder = { Text(if (isDuration) "1:00" else if (lowerIsBetter) "2" else "5") },
                        supportingText = {
                            Text(
                                if (goalError) {
                                    if (isDuration) "Use h:mm, like 1:30" else "Enter a number above 0"
                                } else if (lowerIsBetter) {
                                    "Days fade out as they near the limit, and are empty at it"
                                } else "Days that reach the goal show full color in the calendar"
                            )
                        },
                        isError = goalError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isDuration) KeyboardType.Text else KeyboardType.Decimal
                        )
                    )
                }

                if (onArchive != null) {
                    TextButton(onClick = onArchive, enabled = !saving) {
                        Text("Archive activity", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        ActivityParams(
                            name = name.trim(),
                            category = category.trim(),
                            kind = kind,
                            unit = if (kind == ActivityKind.NUMBER) unit.trim() else "",
                            goal = parsedGoal,
                            lowerIsBetter = lowerIsBetter,
                            sortOrder = activity?.sortOrder ?: 0
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

@Preview
@Composable
fun PreviewActivityEditDialog() {
    ActivityEditDialog(
        activity = null,
        categories = listOf("Food and Drink", "Sports"),
        onDismiss = {},
        onSave = {}
    )
}
