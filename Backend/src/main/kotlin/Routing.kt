package com.mushusax

import com.mushusax.model.*
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
        post("/login") {
            val loginParams = call.receive<LoginParams>()
            val user = withTransaction {
                UserDAO.find { UsersTable.username eq loginParams.username }.map { dAO -> daoToModel(dAO) }
                    .singleOrNull()
            }
            if (user != null) {
                call.respond(message = user, status = HttpStatusCode.OK)
            } else {
                call.respond(message = HttpStatusCode.NotFound)
            }
        }

        post("/logout") {
            val logoutParams = call.receive<LogoutParams>()
            val user = withTransaction {
                UserDAO.findById(logoutParams.user.id)
            }
            if (user != null) {
                call.respond(message = HttpStatusCode.OK)
            } else {
                call.respond(message = HttpStatusCode.NotFound)
            }
        }
    }
}

@Serializable
data class LoginParams(val username: String)

@Serializable
data class LogoutParams(val user: User)