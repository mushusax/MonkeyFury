package com.mushusax

import com.mushusax.model.Activity
import com.mushusax.model.ActivityEntry
import com.mushusax.model.getUser
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.*

class ServerTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val dbFile = File.createTempFile("monkeyfury-test", ".db").apply { deleteOnExit() }

    /** Starts the app on a throwaway database and hands the test a signed token per user. */
    private fun withApp(block: suspend ApplicationTestBuilder.(tokenFor: suspend (String) -> String) -> Unit) =
        testApplication {
            val config = ApplicationConfig("application.yaml").mergeWith(
                MapApplicationConfig("database.url" to "jdbc:sqlite:${dbFile.path}?foreign_keys=on")
            )
            environment { this.config = config }
            startApplication()
            val jwt = JwtConfig(config)
            block { sub ->
                val login = getUser(googleSub = sub, email = "$sub@example.com", firstName = sub, lastName = "")
                issueSessionToken(jwt, login.user.publicId, login.sessionVersion)
            }
        }

    private suspend fun HttpClient.send(
        method: HttpMethod, path: String, token: String, body: String? = null
    ): HttpResponse = request(path) {
        this.method = method
        bearerAuth(token)
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    @Test
    fun `activities require a session`() = withApp {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/activities").status)
    }

    @Test
    fun `logout ends every existing session but a new login works`() = withApp { tokenFor ->
        val phone = tokenFor("frank")
        val tablet = tokenFor("frank")
        assertEquals(HttpStatusCode.OK, client.send(HttpMethod.Get, "/me", phone).status)

        assertEquals(HttpStatusCode.NoContent, client.send(HttpMethod.Post, "/auth/logout", phone).status)
        assertEquals(HttpStatusCode.Unauthorized, client.send(HttpMethod.Get, "/me", phone).status)
        assertEquals(HttpStatusCode.Unauthorized, client.send(HttpMethod.Get, "/activities", tablet).status)
        // Logging out again with the dead token is refused rather than bumping the version twice
        assertEquals(HttpStatusCode.Unauthorized, client.send(HttpMethod.Post, "/auth/logout", phone).status)

        val fresh = tokenFor("frank")
        assertEquals(HttpStatusCode.OK, client.send(HttpMethod.Get, "/me", fresh).status)
    }

    @Test
    fun `tokens without a session version are refused`() = withApp { tokenFor ->
        tokenFor("gina")
        val jwt = JwtConfig(ApplicationConfig("application.yaml"))
        val publicId = client.send(HttpMethod.Get, "/me", tokenFor("gina")).bodyAsText()
            .let { json.decodeFromString<com.mushusax.model.User>(it).publicId }
        val legacy = com.auth0.jwt.JWT.create()
            .withIssuer(jwt.issuer)
            .withAudience(jwt.audience)
            .withSubject(publicId.toString())
            .sign(jwt.algorithm)
        assertEquals(HttpStatusCode.Unauthorized, client.send(HttpMethod.Get, "/me", legacy).status)
    }

    @Test
    fun `create, record, read back, and archive an activity`() = withApp { tokenFor ->
        val token = tokenFor("alice")

        val created = client.send(
            HttpMethod.Post, "/activities", token,
            """{"name":"Running","category":"Sports","kind":"DURATION","goal":60}"""
        )
        assertEquals(HttpStatusCode.Created, created.status)
        val activity = json.decodeFromString<Activity>(created.bodyAsText())
        assertEquals("", activity.unit)

        val saved = client.send(
            HttpMethod.Put, "/activities/${activity.publicId}/entries/2026-11-18", token, """{"value":45.4}"""
        )
        assertEquals(HttpStatusCode.OK, saved.status)
        // DURATION values are whole minutes
        assertEquals(45.0, json.decodeFromString<ActivityEntry>(saved.bodyAsText()).value)

        // Saving the same day again replaces the value
        client.send(HttpMethod.Put, "/activities/${activity.publicId}/entries/2026-11-18", token, """{"value":30}""")

        val entries = client.send(HttpMethod.Get, "/activities/entries?from=2026-11-01&to=2026-11-30", token)
        val list = json.decodeFromString<List<ActivityEntry>>(entries.bodyAsText())
        assertEquals(1, list.size)
        assertEquals(30.0, list.single().value)

        assertEquals(
            HttpStatusCode.NoContent,
            client.send(HttpMethod.Delete, "/activities/${activity.publicId}/entries/2026-11-18", token).status
        )
        assertEquals(
            "[]",
            client.send(HttpMethod.Get, "/activities/entries?from=2026-11-01&to=2026-11-30", token).bodyAsText()
        )

        assertEquals(HttpStatusCode.NoContent, client.send(HttpMethod.Delete, "/activities/${activity.publicId}", token).status)
        assertEquals("[]", client.send(HttpMethod.Get, "/activities", token).bodyAsText())
    }

    @Test
    fun `new columns are added to tables created before they existed`() {
        // The users and activities tables as earlier releases created them
        java.sql.DriverManager.getConnection("jdbc:sqlite:${dbFile.path}").use { connection ->
            connection.createStatement().use {
                it.executeUpdate(
                    "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, public_id BLOB NOT NULL UNIQUE, " +
                        "google_sub VARCHAR(255) NOT NULL UNIQUE, email VARCHAR(320) NOT NULL, " +
                        "first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) DEFAULT '' NOT NULL, " +
                        "created_at TEXT DEFAULT (CURRENT_TIMESTAMP) NOT NULL)"
                )
                it.executeUpdate(
                    "CREATE TABLE activities (id INTEGER PRIMARY KEY AUTOINCREMENT, public_id BLOB NOT NULL, " +
                        "user_public_id BLOB NOT NULL, name VARCHAR(100) NOT NULL, category VARCHAR(100) DEFAULT '' NOT NULL, " +
                        "kind VARCHAR(16) NOT NULL, unit VARCHAR(20) DEFAULT '' NOT NULL, goal DOUBLE PRECISION NULL, " +
                        "sort_order INT DEFAULT 0 NOT NULL, archived BOOLEAN DEFAULT 0 NOT NULL, " +
                        "created_at TEXT DEFAULT (CURRENT_TIMESTAMP) NOT NULL)"
                )
            }
        }
        withApp { tokenFor ->
            val token = tokenFor("erin")
            val created = client.send(HttpMethod.Post, "/activities", token, """{"name":"Fruits","kind":"NUMBER"}""")
            assertEquals(HttpStatusCode.Created, created.status)
            assertFalse(json.decodeFromString<Activity>(created.bodyAsText()).lowerIsBetter)
        }
    }

    @Test
    fun `lower is better is saved and can be changed`() = withApp { tokenFor ->
        val token = tokenFor("dave")
        val created = client.send(
            HttpMethod.Post, "/activities", token,
            """{"name":"Drinks","kind":"NUMBER","unit":"drinks","goal":2,"lowerIsBetter":true}"""
        )
        val activity = json.decodeFromString<Activity>(created.bodyAsText())
        assertTrue(activity.lowerIsBetter)

        val updated = client.send(
            HttpMethod.Put, "/activities/${activity.publicId}", token,
            """{"name":"Drinks","kind":"NUMBER","unit":"drinks","goal":2}"""
        )
        assertFalse(json.decodeFromString<Activity>(updated.bodyAsText()).lowerIsBetter)
    }

    @Test
    fun `users cannot see or touch each other's activities`() = withApp { tokenFor ->
        val alice = tokenFor("alice2")
        val bob = tokenFor("bob")
        val created = client.send(HttpMethod.Post, "/activities", alice, """{"name":"Fruits","kind":"NUMBER"}""")
        val activity = json.decodeFromString<Activity>(created.bodyAsText())

        assertEquals("[]", client.send(HttpMethod.Get, "/activities", bob).bodyAsText())
        assertEquals(
            HttpStatusCode.NotFound,
            client.send(HttpMethod.Put, "/activities/${activity.publicId}/entries/2026-11-18", bob, """{"value":1}""").status
        )
        assertEquals(HttpStatusCode.NotFound, client.send(HttpMethod.Delete, "/activities/${activity.publicId}", bob).status)
    }

    @Test
    fun `bad input is rejected with 400`() = withApp { tokenFor ->
        val token = tokenFor("carol")
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Post, "/activities", token, """{"name":"  ","kind":"NUMBER"}""").status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Get, "/activities/entries?from=2026-11-30&to=2026-11-01", token).status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Get, "/activities/entries?from=nope&to=2026-11-01", token).status
        )
    }
}
