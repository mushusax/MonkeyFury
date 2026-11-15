package com.mushusax

import com.mushusax.model.UsersTable
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
        SchemaUtils.create(UsersTable)
    }
}
