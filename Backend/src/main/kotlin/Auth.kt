package com.mushusax

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.mushusax.model.User
import com.mushusax.model.endSessions
import com.mushusax.model.getUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

//The JWT secret is generated and kept in memory for now
private val sessionAlgorithm: Algorithm =
    Algorithm.HMAC256(ByteArray(64).also(SecureRandom()::nextBytes))

class JwtConfig(config: ApplicationConfig) {
    val issuer = config.property("auth.jwt.issuer").getString()
    val audience = config.property("auth.jwt.audience").getString()
    val validity = config.property("auth.jwt.validityDays").getString().toLong().days
    val algorithm: Algorithm = sessionAlgorithm
}

fun Application.configureAuth() {
    val config = environment.config
    val googleWebClientId = config.property("auth.google.webClientId").getString()
    val jwt = JwtConfig(config)
    val googleVerifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(listOf(googleWebClientId))
        .build()

    routing {
        post("/auth/google") {
            val params = call.receive<GoogleLoginParams>()

            val payload = verifyGoogleIdToken(googleVerifier, params.idToken)
            if (payload == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            val email = payload.email
            if (email.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Google account has no email")
                return@post
            }

            val login = getUser(
                googleSub = payload.subject,
                email = email,
                firstName = payload["given_name"] as? String ?: payload["name"] as? String ?: "",
                lastName = payload["family_name"] as? String ?: ""
            )

            val token = issueSessionToken(jwt, login.user.publicId, login.sessionVersion)
            call.respond(LoginResponse(token, login.user))
        }

        authenticate("session") {
            // Ends every session for this user, so all their tokens stop working
            post("/auth/logout") {
                endSessions(call.principal<UserSession>()!!.publicId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/** The JWT claim holding the user's session version at the time the token was issued. */
const val SESSION_VERSION_CLAIM = "sv"

fun issueSessionToken(jwt: JwtConfig, publicId: Uuid, sessionVersion: Int): String =
    JWT.create()
        .withIssuer(jwt.issuer)
        .withAudience(jwt.audience)
        .withSubject(publicId.toString())
        .withClaim(SESSION_VERSION_CLAIM, sessionVersion)
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + jwt.validity.inWholeMilliseconds))
        .sign(jwt.algorithm)

private suspend fun verifyGoogleIdToken(verifier: GoogleIdTokenVerifier, idToken: String): GoogleIdToken.Payload? =
    withContext(Dispatchers.IO) {
        try {
            verifier.verify(idToken)?.payload
        } catch (_: Exception) {
            null
        }
    }

data class UserSession(val publicId: Uuid)

@Serializable
data class GoogleLoginParams(val idToken: String)

@Serializable
data class LoginResponse(val token: String, val user: User)
