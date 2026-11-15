package com.mushusax

import com.mushusax.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.eq

fun Application.configureRouting() {
    routing {
        authenticate("session") {
            get("/me") {
                val publicId = call.principal<UserSession>()!!.publicId
                val user = withTransaction {
                    UserDAO.find { UsersTable.publicId eq publicId }.singleOrNull()?.let(::daoToModel)
                }
                if (user != null) {
                    call.respond(user)
                } else {
                    // Token is valid but the user no longer exists (e.g. database was reset).
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
