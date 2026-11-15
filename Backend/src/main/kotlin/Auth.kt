package com.mushusax

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.mushusax.model.User
import com.mushusax.model.getUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

class JwtConfig(config: ApplicationConfig) {
    val issuer = config.property("auth.jwt.issuer").getString()
    val audience = config.property("auth.jwt.audience").getString()
    val validity = config.property("auth.jwt.validityDays").getString().toLong().days
    val algorithm: Algorithm = Algorithm.HMAC256(config.property("auth.jwt.secret").getString())
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

            val user = getUser(
                googleSub = payload.subject,
                email = email,
                firstName = payload["given_name"] as? String ?: payload["name"] as? String ?: "",
                lastName = payload["family_name"] as? String ?: ""
            )

            val token = JWT.create()
                .withIssuer(jwt.issuer)
                .withAudience(jwt.audience)
                .withSubject(user.publicId.toString())
                .withIssuedAt(Date())
                .withExpiresAt(Date(System.currentTimeMillis() + jwt.validity.inWholeMilliseconds))
                .sign(jwt.algorithm)

            call.respond(LoginResponse(token, user))
        }
    }
}

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
