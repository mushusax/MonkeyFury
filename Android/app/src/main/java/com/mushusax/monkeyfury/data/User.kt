package com.mushusax.monkeyfury.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val publicId: String,
    val email: String,
    val firstName: String,
    val lastName: String
)
