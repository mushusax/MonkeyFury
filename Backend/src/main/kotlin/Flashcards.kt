package com.mushusax

import com.mushusax.model.listFlashcards
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureFlashcards() {
    routing {
        authenticate("session") {
            get("/flashcards") {
                call.respond(listFlashcards(call.principal<UserSession>()!!.publicId))
            }
        }
    }
}
