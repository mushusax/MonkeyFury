package com.mushusax

import com.mushusax.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun Application.configureFlashcards() {
    routing {
        authenticate("session") {
            // ?zone=America/Chicago sets where "today" begins for the new-card count; UTC when left out
            get("/flashcards") {
                val zone = call.request.queryParameters["zone"]?.let {
                    try {
                        ZoneId.of(it)
                    } catch (_: DateTimeException) {
                        throw BadRequestException("Unknown time zone '$it'")
                    }
                } ?: ZoneOffset.UTC
                val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
                call.respond(listFlashcards(call.userPublicId(), Instant.fromEpochMilliseconds(startOfToday)))
            }

            post("/cards/{id}/answer") {
                val params = call.receive<AnswerParams>()
                val card = answerCard(call.userPublicId(), call.uuidParameter("id", "card"), params.rating)
                if (card == null) call.respond(HttpStatusCode.NotFound) else call.respond(card)
            }

            post("/decks/{id}/notes") {
                val params = call.receive<NoteParams>()
                val result = createNote(call.userPublicId(), call.uuidParameter("id", "deck"), params)
                if (result == null) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.Created, result)
            }

            route("/notes/{id}") {
                put {
                    val params = call.receive<NoteParams>()
                    val result = updateNote(call.userPublicId(), call.uuidParameter("id", "note"), params)
                    if (result == null) call.respond(HttpStatusCode.NotFound) else call.respond(result)
                }

                delete {
                    val deleted = deleteNote(call.userPublicId(), call.uuidParameter("id", "note"))
                    call.respond(if (deleted) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
                }
            }
        }
    }
}

private fun ApplicationCall.userPublicId(): Uuid = principal<UserSession>()!!.publicId

private fun ApplicationCall.uuidParameter(name: String, what: String): Uuid =
    parameters[name]?.let(Uuid::parseOrNull) ?: throw BadRequestException("Invalid $what id")
