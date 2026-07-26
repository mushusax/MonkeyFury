package com.mushusax

import com.mushusax.model.User
import com.mushusax.model.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureExposed() {
    Database.connect(
        url = "jdbc:sqlite:app.db",
        driver = "org.sqlite.JDBC"
    )
    transaction {
        SchemaUtils.create(Users)
        User.new {
            username = "example"
            name = "John Doe"
            email = "example@example.com"
        }
    }
}