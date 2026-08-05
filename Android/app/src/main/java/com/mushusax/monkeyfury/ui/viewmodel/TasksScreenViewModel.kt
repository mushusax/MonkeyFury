package com.mushusax.monkeyfury.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel

class TasksScreenViewModel(application: Application) : AndroidViewModel(application) {
    val tasks = mutableStateListOf<String>()

    fun addTask(task: String) {
        tasks.add(task)
    }
}