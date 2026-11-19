package com.mushusax

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import com.mushusax.model.sessionVersion
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
        // Malformed bodies and params; also thrown by require() checks in the model
        exception<BadRequestException> { call, cause ->
            call.respondText(cause.message ?: "Bad request", status = HttpStatusCode.BadRequest)
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respondText(cause.message ?: "Bad request", status = HttpStatusCode.BadRequest)
        }
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
            // Valid only while the token's session version matches the user's; logout bumps it
            validate { credential ->
                val publicId = credential.subject?.let(Uuid::parseOrNull) ?: return@validate null
                val tokenVersion = credential.payload.getClaim(SESSION_VERSION_CLAIM).asInt() ?: return@validate null
                if (sessionVersion(publicId) == tokenVersion) UserSession(publicId) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
