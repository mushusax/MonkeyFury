package com.mushusax

import com.mushusax.model.User
import com.mushusax.model.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureRouting() {
    install(StatusPages) {
        exception<IllegalStateException> { call, cause ->
            call.respondText("App in illegal state as ${cause.message}")
        }
    }

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        post("/login") {
            val username = call.receiveText()
            val user = transaction { User.find { Users.username eq username }.singleOrNull() }

            if (user != null) {
                call.respondText(status = HttpStatusCode.Found) {
                    user.toString()
                }
            } else {
                call.respond(status = HttpStatusCode.NotFound, "User not found")
            }
        }
    }
}
