package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.R
import com.mushusax.monkeyfury.ui.viewmodel.TasksScreenViewModel

@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksScreenViewModel = viewModel()
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        val taskTextFieldState = rememberTextFieldState()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(1f)
                .weight(1f)
        ) {
            items(items = viewModel.tasks) { task ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(16.dp),
                    text = task
                )
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(1f),
            state = taskTextFieldState,
            label = {
                Text(text = "Enter Task")
            },
            trailingIcon = {
                IconButton(onClick = {
                    viewModel.addTask(taskTextFieldState.text.toString())
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_add),
                        contentDescription = "Add Task"
                    )
                }
            }
        )
    }
}

@Preview
@Composable
fun PreviewTasksScreen() {
    var tasks = mutableListOf<String>()
    tasks.add("Task 1")
    tasks.add("Task 2")
    tasks.add("Task 3")
    tasks.add("Task 4")
    tasks.add("Task 5")
    tasks.add("Task 6")
    tasks.add("Task 7")
    TasksScreen(
        modifier = Modifier
            .fillMaxWidth(1f)
            .fillMaxHeight(1f)
    )
}