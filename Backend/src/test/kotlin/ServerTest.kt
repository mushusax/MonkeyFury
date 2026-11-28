package com.mushusax

import com.mushusax.model.Activity
import com.mushusax.model.ActivityEntry
import com.mushusax.model.Card
import com.mushusax.model.CardDirection
import com.mushusax.model.CardState
import com.mushusax.model.ReviewLogsTable
import com.mushusax.model.CardsTable
import com.mushusax.model.DecksTable
import com.mushusax.model.FlashcardData
import com.mushusax.model.Media
import com.mushusax.model.MediaLogEntry
import com.mushusax.model.MediaStatus
import com.mushusax.model.NoteResult
import com.mushusax.model.NotesTable
import com.mushusax.model.User
import com.mushusax.model.getUser
import com.mushusax.model.withTransaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
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

    @Test
    fun `flashcards are returned only to their owner`() = withApp { tokenFor ->
        val owner = tokenFor("hana")
        val other = tokenFor("ivan")
        val ownerId = json.decodeFromString<User>(client.send(HttpMethod.Get, "/me", owner).bodyAsText()).publicId
        withTransaction {
            val deck = DecksTable.insertAndGetId {
                it[userPublicId] = ownerId
                it[name] = "Example Deck"
            }
            val note = NotesTable.insertAndGetId {
                it[deckId] = deck
                it[front] = "Example Card 1"
                it[back] = "Example Answer 1"
            }
            CardsTable.insertAndGetId {
                it[noteId] = note
                it[direction] = CardDirection.FORWARD
            }
        }

        val response = client.send(HttpMethod.Get, "/flashcards", owner)
        assertEquals(HttpStatusCode.OK, response.status)
        val data = json.decodeFromString<FlashcardData>(response.bodyAsText())
        val deck = data.decks.single()
        val note = data.notes.single()
        assertEquals("Example Deck", deck.name)
        assertEquals(deck.publicId, note.deckId)
        assertEquals("Example Card 1", note.front)
        assertEquals(note.publicId, data.cards.single().noteId)

        val empty = json.decodeFromString<FlashcardData>(client.send(HttpMethod.Get, "/flashcards", other).bodyAsText())
        assertTrue(empty.decks.isEmpty() && empty.notes.isEmpty() && empty.cards.isEmpty())
        assertEquals(HttpStatusCode.Unauthorized, client.get("/flashcards").status)
    }

    /** Inserts an empty deck for the user behind [token] and returns its public id. */
    private suspend fun ApplicationTestBuilder.insertDeck(token: String): String {
        val userId = json.decodeFromString<User>(client.send(HttpMethod.Get, "/me", token).bodyAsText()).publicId
        val deckId = withTransaction {
            DecksTable.insertAndGetId {
                it[userPublicId] = userId
                it[name] = "Deck"
            }
        }
        return withTransaction {
            DecksTable.selectAll().where { DecksTable.id eq deckId }.single()[DecksTable.publicId].toString()
        }
    }

    @Test
    fun `notes are created, made reversible, made one-way, and deleted`() = withApp { tokenFor ->
        val token = tokenFor("jun")
        val deckId = insertDeck(token)

        val created = client.send(
            HttpMethod.Post, "/decks/$deckId/notes", token, """{"front":" perro ","back":"dog"}"""
        )
        assertEquals(HttpStatusCode.Created, created.status)
        val note = json.decodeFromString<NoteResult>(created.bodyAsText())
        assertEquals("perro", note.note.front)
        assertEquals(listOf(CardDirection.FORWARD), note.cards.map { it.direction })

        val reversible = client.send(
            HttpMethod.Put, "/notes/${note.note.publicId}", token, """{"front":"perro","back":"dog","reversible":true}"""
        )
        val both = json.decodeFromString<NoteResult>(reversible.bodyAsText())
        assertEquals(listOf(CardDirection.FORWARD, CardDirection.REVERSE), both.cards.map { it.direction })
        // The forward card keeps its schedule
        assertEquals(note.cards.single().publicId, both.cards.first().publicId)

        val oneWay = client.send(
            HttpMethod.Put, "/notes/${note.note.publicId}", token, """{"front":"perro","back":"the dog"}"""
        )
        val forwardOnly = json.decodeFromString<NoteResult>(oneWay.bodyAsText())
        assertEquals("the dog", forwardOnly.note.back)
        assertEquals(listOf(CardDirection.FORWARD), forwardOnly.cards.map { it.direction })

        assertEquals(HttpStatusCode.NoContent, client.send(HttpMethod.Delete, "/notes/${note.note.publicId}", token).status)
        val data = json.decodeFromString<FlashcardData>(client.send(HttpMethod.Get, "/flashcards", token).bodyAsText())
        assertTrue(data.notes.isEmpty() && data.cards.isEmpty())
        assertEquals(1, data.decks.size)
    }

    @Test
    fun `users cannot add to or change each other's notes`() = withApp { tokenFor ->
        val owner = tokenFor("kai")
        val other = tokenFor("lea")
        val deckId = insertDeck(owner)
        val note = json.decodeFromString<NoteResult>(
            client.send(HttpMethod.Post, "/decks/$deckId/notes", owner, """{"front":"a","back":"b"}""").bodyAsText()
        ).note

        assertEquals(
            HttpStatusCode.NotFound,
            client.send(HttpMethod.Post, "/decks/$deckId/notes", other, """{"front":"x","back":"y"}""").status
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.send(HttpMethod.Put, "/notes/${note.publicId}", other, """{"front":"x","back":"y"}""").status
        )
        assertEquals(HttpStatusCode.NotFound, client.send(HttpMethod.Delete, "/notes/${note.publicId}", other).status)
    }

    @Test
    fun `blank card text and bad ids are rejected with 400`() = withApp { tokenFor ->
        val token = tokenFor("mia")
        val deckId = insertDeck(token)
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Post, "/decks/$deckId/notes", token, """{"front":"  ","back":"b"}""").status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Put, "/notes/nope", token, """{"front":"a","back":"b"}""").status
        )
    }

    @Test
    fun `answering a card reschedules it with FSRS and logs the answer`() = withApp { tokenFor ->
        val token = tokenFor("nora")
        val deckId = insertDeck(token)
        val card = json.decodeFromString<NoteResult>(
            client.send(HttpMethod.Post, "/decks/$deckId/notes", token, """{"front":"a","back":"b"}""").bodyAsText()
        ).cards.single()

        // Good on a new card moves it to the second learning step, 10 minutes out
        val good = client.send(HttpMethod.Post, "/cards/${card.publicId}/answer", token, """{"rating":"GOOD"}""")
        assertEquals(HttpStatusCode.OK, good.status)
        val learning = json.decodeFromString<Card>(good.bodyAsText())
        assertEquals(CardState.LEARNING, learning.state)
        assertEquals(1, learning.learningStep)
        assertEquals(10 * 60_000L, learning.due - learning.lastReview!!)

        // Easy graduates it to day-based review
        val easy = client.send(HttpMethod.Post, "/cards/${card.publicId}/answer", token, """{"rating":"EASY"}""")
        val review = json.decodeFromString<Card>(easy.bodyAsText())
        assertEquals(CardState.REVIEW, review.state)
        assertTrue(review.due - review.lastReview!! >= 86_400_000L)

        // The schedule is saved, and the first answer counts as one new card today
        val data = json.decodeFromString<FlashcardData>(
            client.send(HttpMethod.Get, "/flashcards?zone=America/Chicago", token).bodyAsText()
        )
        assertEquals(review, data.cards.single())
        assertEquals(1, data.newCardsToday)
        // Both answers land on today in the review calendar
        val day = data.reviewDays.single()
        assertEquals(deckId, day.deckId.toString())
        assertEquals(java.time.LocalDate.now(java.time.ZoneId.of("America/Chicago")).toString(), day.date)
        assertEquals(2, day.count)
        assertEquals(2L, withTransaction { ReviewLogsTable.selectAll().count() })
    }

    @Test
    fun `answers are refused for other users' cards and bad input`() = withApp { tokenFor ->
        val owner = tokenFor("omar")
        val other = tokenFor("pia")
        val deckId = insertDeck(owner)
        val card = json.decodeFromString<NoteResult>(
            client.send(HttpMethod.Post, "/decks/$deckId/notes", owner, """{"front":"a","back":"b"}""").bodyAsText()
        ).cards.single()

        assertEquals(
            HttpStatusCode.NotFound,
            client.send(HttpMethod.Post, "/cards/${card.publicId}/answer", other, """{"rating":"GOOD"}""").status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Post, "/cards/${card.publicId}/answer", owner, """{"rating":"MAYBE"}""").status
        )
        assertEquals(HttpStatusCode.BadRequest, client.send(HttpMethod.Get, "/flashcards?zone=Mars/Base", owner).status)
    }

    private suspend fun ApplicationTestBuilder.createMedia(token: String, body: String): Media {
        val created = client.send(HttpMethod.Post, "/media", token, body)
        assertEquals(HttpStatusCode.Created, created.status)
        return json.decodeFromString<Media>(created.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.logProgress(token: String, id: Any, amount: Int, date: String): Media {
        val logged = client.send(HttpMethod.Post, "/media/$id/progress", token, """{"amount":$amount,"date":"$date"}""")
        assertEquals(HttpStatusCode.OK, logged.status)
        return json.decodeFromString<Media>(logged.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.mediaLog(token: String): List<MediaLogEntry> =
        json.decodeFromString(client.send(HttpMethod.Get, "/media/log?from=2026-11-01&to=2026-11-30", token).bodyAsText())

    @Test
    fun `logging progress starts, adds up by day, completes, and undoes a book`() = withApp { tokenFor ->
        val token = tokenFor("quinn")
        val book = createMedia(token, """{"kind":"BOOK","title":" Dune ","creator":"Frank Herbert","total":100}""")
        assertEquals("Dune", book.title)
        assertEquals(MediaStatus.PLANNED, book.status)

        val started = logProgress(token, book.publicId, 30, "2026-11-20")
        assertEquals(MediaStatus.IN_PROGRESS, started.status)
        assertEquals("2026-11-20", started.startedOn.toString())

        logProgress(token, book.publicId, 20, "2026-11-20")
        // Going past the total stops at the total, and only what was applied is logged
        val done = logProgress(token, book.publicId, 80, "2026-11-21")
        assertEquals(100, done.progress)
        assertEquals(MediaStatus.COMPLETED, done.status)
        assertEquals("2026-11-21", done.finishedOn.toString())
        assertEquals(
            mapOf("2026-11-20" to 50, "2026-11-21" to 50),
            mediaLog(token).associate { it.date.toString() to it.amount }
        )

        val undone = logProgress(token, book.publicId, -50, "2026-11-21")
        assertEquals(MediaStatus.IN_PROGRESS, undone.status)
        assertNull(undone.finishedOn)
        assertEquals(listOf("2026-11-20"), mediaLog(token).map { it.date.toString() })

        assertEquals(HttpStatusCode.NoContent, client.send(HttpMethod.Delete, "/media/${book.publicId}", token).status)
        assertEquals("[]", client.send(HttpMethod.Get, "/media", token).bodyAsText())
        assertEquals(emptyList(), mediaLog(token))
    }

    @Test
    fun `undoing takes back at most what was logged that day`() = withApp { tokenFor ->
        val token = tokenFor("sam")
        val book = createMedia(token, """{"kind":"BOOK","title":"Emma","progress":40,"total":300}""")
        logProgress(token, book.publicId, 10, "2026-11-20")

        // Only 10 were logged that day, so a bigger undo stops there instead of eating the hand-set 40
        val undone = logProgress(token, book.publicId, -25, "2026-11-20")
        assertEquals(40, undone.progress)
        assertEquals(emptyList(), mediaLog(token))

        // Nothing logged on a day means nothing to undo there
        assertEquals(40, logProgress(token, book.publicId, -1, "2026-11-21").progress)
        assertEquals(emptyList(), mediaLog(token))
    }

    @Test
    fun `a completed movie is logged once on the day it was finished`() = withApp { tokenFor ->
        val token = tokenFor("rosa")
        val movie = createMedia(
            token,
            """{"kind":"MOVIE","title":"Arrival","status":"COMPLETED","thumbsUp":true,"finishedOn":"2026-11-10","progress":5,"total":9}"""
        )
        assertEquals(0, movie.progress)
        assertNull(movie.total)
        assertEquals(true, movie.thumbsUp)
        assertEquals(listOf("2026-11-10" to 1), mediaLog(token).map { it.date.toString() to it.amount })

        // Moving the finished day moves the log row rather than adding another
        val moved = client.send(
            HttpMethod.Put, "/media/${movie.publicId}", token,
            """{"kind":"MOVIE","title":"Arrival","status":"COMPLETED","finishedOn":"2026-11-12"}"""
        )
        assertEquals(HttpStatusCode.OK, moved.status)
        assertEquals(listOf("2026-11-12" to 1), mediaLog(token).map { it.date.toString() to it.amount })

        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Post, "/media/${movie.publicId}/progress", token, """{"amount":1,"date":"2026-11-12"}""").status
        )
    }

    @Test
    fun `users cannot see or touch each other's media`() = withApp { tokenFor ->
        val owner = tokenFor("sam")
        val other = tokenFor("tess")
        val anime = createMedia(owner, """{"kind":"ANIME","title":"Frieren","total":28}""")
        logProgress(owner, anime.publicId, 1, "2026-11-25")

        assertEquals("[]", client.send(HttpMethod.Get, "/media", other).bodyAsText())
        assertEquals(emptyList(), mediaLog(other))
        assertEquals(
            HttpStatusCode.NotFound,
            client.send(HttpMethod.Put, "/media/${anime.publicId}", other, """{"kind":"ANIME","title":"Mine"}""").status
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.send(HttpMethod.Post, "/media/${anime.publicId}/progress", other, """{"amount":1,"date":"2026-11-25"}""").status
        )
        assertEquals(HttpStatusCode.NotFound, client.send(HttpMethod.Delete, "/media/${anime.publicId}", other).status)
    }

    @Test
    fun `bad media input is rejected with 400`() = withApp { tokenFor ->
        val token = tokenFor("uma")
        val bad = listOf(
            """{"kind":"BOOK","title":"  "}""",
            """{"kind":"BOOK","title":"A","progress":11,"total":10}""",
            """{"kind":"BOOK","title":"A","total":0}""",
            """{"kind":"BOOK","title":"A","startedOn":"2026-11-10","finishedOn":"2026-11-01"}""",
            """{"kind":"PODCAST","title":"A"}"""
        )
        for (body in bad) assertEquals(HttpStatusCode.BadRequest, client.send(HttpMethod.Post, "/media", token, body).status, body)

        val book = createMedia(token, """{"kind":"BOOK","title":"A"}""")
        assertEquals(
            HttpStatusCode.BadRequest,
            client.send(HttpMethod.Post, "/media/${book.publicId}/progress", token, """{"amount":0,"date":"2026-11-01"}""").status
        )
        assertEquals(HttpStatusCode.BadRequest, client.send(HttpMethod.Get, "/media/log?from=2026-11-30&to=2026-11-01", token).status)
        assertEquals(HttpStatusCode.BadRequest, client.send(HttpMethod.Put, "/media/nope", token, """{"kind":"BOOK","title":"A"}""").status)
    }
}
