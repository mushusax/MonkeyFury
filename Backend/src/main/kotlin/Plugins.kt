package com.mushusax

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.uuid.Uuid

fun Application.configurePlugins() {
    val jwt = JwtConfig(environment.config)

    install(CallLogging) {
        logger = LoggerFactory.getLogger("requests")
        level = Level.INFO
        disableDefaultColors()
        format { call -> "${call.request.httpMethod.value} ${call.request.path()}" }
    }

    install(StatusPages) {
        exception<IllegalStateException> { call, cause ->
            call.respondText("App in illegal state as ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(Authentication) {
        jwt("session") {
            realm = "munkyfury"
            verifier(
                JWT.require(jwt.algorithm)
                    .withIssuer(jwt.issuer)
                    .withAudience(jwt.audience)
                    .build()
            )
            validate { credential ->
                credential.subject?.let(Uuid::parseOrNull)?.let(::UserSession)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
