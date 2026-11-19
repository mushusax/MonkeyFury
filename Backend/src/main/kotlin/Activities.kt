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

/** The widest range GET /activities/entries will return, so one request can't pull years of data. */
private const val MAX_RANGE_DAYS = 400

fun Application.configureActivities() {
    routing {
        authenticate("session") {
            route("/activities") {
                get {
                    call.respond(listActivities(call.userPublicId()))
                }

                post {
                    val params = call.receive<ActivityParams>()
                    call.respond(HttpStatusCode.Created, createActivity(call.userPublicId(), params))
                }

                // Declared before /{id} so "entries" isn't read as an activity id
                get("/entries") {
                    val from = call.dateParameter("from")
                    val to = call.dateParameter("to")
                    if (to < from) throw BadRequestException("'to' is before 'from'")
                    if (to.toEpochDays() - from.toEpochDays() > MAX_RANGE_DAYS) {
                        throw BadRequestException("Range is longer than $MAX_RANGE_DAYS days")
                    }
                    call.respond(listEntries(call.userPublicId(), from, to))
                }

                route("/{id}") {
                    put {
                        val params = call.receive<ActivityParams>()
                        val activity = updateActivity(call.userPublicId(), call.activityId(), params)
                        if (activity == null) call.respond(HttpStatusCode.NotFound) else call.respond(activity)
                    }

                    delete {
                        val archived = archiveActivity(call.userPublicId(), call.activityId())
                        call.respond(if (archived) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
                    }

                    route("/entries/{date}") {
                        put {
                            val params = call.receive<EntryValueParams>()
                            val entry = saveEntry(
                                call.userPublicId(), call.activityId(), call.dateParameter("date"), params.value
                            )
                            if (entry == null) call.respond(HttpStatusCode.NotFound) else call.respond(entry)
                        }

                        delete {
                            val found = deleteEntry(call.userPublicId(), call.activityId(), call.dateParameter("date"))
                            call.respond(if (found) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.userPublicId(): Uuid = principal<UserSession>()!!.publicId

private fun ApplicationCall.activityId(): Uuid =
    parameters["id"]?.let(Uuid::parseOrNull) ?: throw BadRequestException("Invalid activity id")

/** Reads a yyyy-MM-dd value from the path or the query string. */
private fun ApplicationCall.dateParameter(name: String): LocalDate {
    val raw = parameters[name] ?: request.queryParameters[name] ?: throw BadRequestException("Missing '$name'")
    return try {
        LocalDate.parse(raw)
    } catch (_: IllegalArgumentException) {
        throw BadRequestException("'$name' must be yyyy-MM-dd")
    }
}
