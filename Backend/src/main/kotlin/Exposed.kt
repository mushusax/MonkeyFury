package com.mushusax

import com.mushusax.model.ActivitiesTable
import com.mushusax.model.ActivityEntriesTable
import com.mushusax.model.CardsTable
import com.mushusax.model.DecksTable
import com.mushusax.model.NotesTable
import com.mushusax.model.ReviewLogsTable
import com.mushusax.model.UsersTable
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureExposed() {
    Database.connect(
        url = environment.config.property("database.url").getString(),
        driver = "org.sqlite.JDBC"
    )
    transaction {
        SchemaUtils.create(
            UsersTable, ActivitiesTable, ActivityEntriesTable,
            DecksTable, NotesTable, CardsTable, ReviewLogsTable
        )
        // create() skips tables that already exist, so columns added later are added here
        SchemaUtils.addMissingColumnsStatements(UsersTable, ActivitiesTable).forEach { exec(it) }
    }
}
