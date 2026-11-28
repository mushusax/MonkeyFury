package com.mushusax

import com.mushusax.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

/** The widest range GET /media/log will return, so one request can't pull years of data. */
private const val MAX_RANGE_DAYS = 400

fun Application.configureMedia() {
    routing {
        authenticate("session") {
            route("/media") {
                get {
                    call.respond(listMedia(call.userPublicId()))
                }

                post {
                    val params = call.receive<MediaParams>()
                    call.respond(HttpStatusCode.Created, createMedia(call.userPublicId(), params))
                }

                // Declared before /{id} so "log" isn't read as a media id
                get("/log") {
                    val from = call.dateQuery("from")
                    val to = call.dateQuery("to")
                    if (to < from) throw BadRequestException("'to' is before 'from'")
                    if (to.toEpochDays() - from.toEpochDays() > MAX_RANGE_DAYS) {
                        throw BadRequestException("Range is longer than $MAX_RANGE_DAYS days")
                    }
                    call.respond(listMediaLog(call.userPublicId(), from, to))
                }

                route("/{id}") {
                    put {
                        val params = call.receive<MediaParams>()
                        val media = updateMedia(call.userPublicId(), call.mediaId(), params)
                        if (media == null) call.respond(HttpStatusCode.NotFound) else call.respond(media)
                    }

                    delete {
                        val archived = archiveMedia(call.userPublicId(), call.mediaId())
                        call.respond(if (archived) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
                    }

                    post("/progress") {
                        val params = call.receive<ProgressParams>()
                        val media = logProgress(call.userPublicId(), call.mediaId(), params)
                        if (media == null) call.respond(HttpStatusCode.NotFound) else call.respond(media)
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.userPublicId(): Uuid = principal<UserSession>()!!.publicId

private fun ApplicationCall.mediaId(): Uuid =
    parameters["id"]?.let(Uuid::parseOrNull) ?: throw BadRequestException("Invalid media id")

/** Reads a yyyy-MM-dd value from the query string. */
private fun ApplicationCall.dateQuery(name: String): LocalDate {
    val raw = request.queryParameters[name] ?: throw BadRequestException("Missing '$name'")
    return try {
        LocalDate.parse(raw)
    } catch (_: IllegalArgumentException) {
        throw BadRequestException("'$name' must be yyyy-MM-dd")
    }
}
