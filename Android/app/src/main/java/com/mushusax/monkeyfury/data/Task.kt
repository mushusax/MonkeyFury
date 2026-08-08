package com.mushusax.monkeyfury.data

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)