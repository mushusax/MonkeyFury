package com.mushusax

import com.mushusax.model.UserDAO
import com.mushusax.model.UsersTable
import com.mushusax.model.daoToModel
import com.mushusax.model.withTransaction
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq

fun Application.configureRouting() {
    install(StatusPages) {
        exception<IllegalStateException> { call, cause ->
            call.respondText("App in illegal state as ${cause.message}")
        }
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        post("/login") {
            val loginParams = call.receive<LoginParams>()
            val user = withTransaction {
                UserDAO.find { UsersTable.username eq loginParams.username }.map { dAO -> daoToModel(dAO) }
                    .singleOrNull()
            }
            if (user != null) {
                call.respond(message = user, status = HttpStatusCode.OK)
            } else {
                call.respond(status = HttpStatusCode.NotFound, "User not found")
            }
        }
    }
}

@Serializable
data class LoginParams(val username: String)


