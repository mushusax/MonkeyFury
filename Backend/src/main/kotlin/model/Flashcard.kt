package com.mushusax.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

// FSRS flashcards: a deck holds notes (what you write); each note is studied as one card per
// direction, and every answer to a card is kept as a review log. Deleting a deck cascades to all of it.

enum class CardDirection { FORWARD, REVERSE }

enum class CardState { NEW, LEARNING, REVIEW, RELEARNING }

enum class Rating(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

object DecksTable : IntIdTable("flashcard_decks") {
    val publicId = uuid("public_id").uniqueIndex().clientDefault { Uuid.random() }
    val userPublicId = reference("user_public_id", UsersTable.publicId, onDelete = ReferenceOption.CASCADE).index()
    val name = varchar("name", 100)
    val description = varchar("description", 500).default("")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

/** What you write. A reversible note has a REVERSE card as well as its FORWARD one. */
object NotesTable : IntIdTable("flashcard_notes") {
    val publicId = uuid("public_id").uniqueIndex().clientDefault { Uuid.random() }
    val deckId = reference("deck_id", DecksTable, onDelete = ReferenceOption.CASCADE).index()
    val front = text("front")
    val back = text("back")
    val reversible = bool("reversible").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

/** What you study: one direction of a note, with its FSRS scheduling state. */
object CardsTable : IntIdTable("flashcard_cards") {
    val publicId = uuid("public_id").uniqueIndex().clientDefault { Uuid.random() }
    val noteId = reference("note_id", NotesTable, onDelete = ReferenceOption.CASCADE)
    val direction = enumerationByName<CardDirection>("direction", 16)
    val state = enumerationByName<CardState>("state", 16).default(CardState.NEW)
    /** Index into the learning or relearning steps while in those states. */
    val learningStep = integer("learning_step").default(0)
    /** FSRS stability in days; null until the first review. */
    val stability = double("stability").nullable()
    /** FSRS difficulty, 1–10; null until the first review. */
    val difficulty = double("difficulty").nullable()
    val due = timestamp("due").defaultExpression(CurrentTimestamp).index()
    val lastReview = timestamp("last_review").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        // At most one card per direction; the index also serves lookups by note
        uniqueIndex(noteId, direction)
    }
}

/** One answer to a card. Only ever added, never edited. */
object ReviewLogsTable : IntIdTable("flashcard_review_logs") {
    val publicId = uuid("public_id").uniqueIndex().clientDefault { Uuid.random() }
    val cardId = reference("card_id", CardsTable, onDelete = ReferenceOption.CASCADE).index()
    val rating = enumerationByName<Rating>("rating", 16)
    /** Indexed for counting today's newly introduced cards against the daily limit. */
    val reviewedAt = timestamp("reviewed_at").index()
    val stateBefore = enumerationByName<CardState>("state_before", 16)
    val elapsedDays = double("elapsed_days")
    val scheduledDays = double("scheduled_days")
    val stabilityAfter = double("stability_after")
    val difficultyAfter = double("difficulty_after")
}

// What the client sees. Ids are public ids, and times are epoch milliseconds, as the app stores them.

@Serializable
data class Deck(
    val publicId: Uuid,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class Note(
    val publicId: Uuid,
    val deckId: Uuid,
    val front: String,
    val back: String,
    val reversible: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class Card(
    val publicId: Uuid,
    val noteId: Uuid,
    val direction: CardDirection,
    val state: CardState,
    val learningStep: Int,
    val stability: Double?,
    val difficulty: Double?,
    val due: Long,
    val lastReview: Long?,
    val updatedAt: Long
)

/** The body of GET /flashcards: every deck the user owns, with all of its notes and cards. */
@Serializable
data class FlashcardData(
    val decks: List<Deck>,
    val notes: List<Note>,
    val cards: List<Card>,
    /** Never-studied cards answered for the first time since the start of today, in the zone asked for. */
    val newCardsToday: Int = 0
)

/** The body of POST /cards/{id}/answer. */
@Serializable
data class AnswerParams(val rating: Rating)

/** The body of POST /decks/{id}/notes and PUT /notes/{id}. */
@Serializable
data class NoteParams(
    val front: String,
    val back: String,
    val reversible: Boolean = false
)

/** A created or updated note with all of its cards after the change. */
@Serializable
data class NoteResult(
    val note: Note,
    val cards: List<Card>
)

/** Front and back are free text; this only stops a runaway paste. */
private const val MAX_SIDE_LENGTH = 10_000

/** Throws IllegalArgumentException (answered with 400) when the params are unusable. */
private fun NoteParams.validated(): NoteParams {
    val front = front.trim()
    val back = back.trim()
    require(front.isNotEmpty()) { "Front is required" }
    require(back.isNotEmpty()) { "Back is required" }
    require(front.length <= MAX_SIDE_LENGTH && back.length <= MAX_SIDE_LENGTH) { "Card text is too long" }
    return NoteParams(front, back, reversible)
}

private fun deckToModel(row: ResultRow) = Deck(
    publicId = row[DecksTable.publicId],
    name = row[DecksTable.name],
    description = row[DecksTable.description],
    createdAt = row[DecksTable.createdAt].toEpochMilliseconds(),
    updatedAt = row[DecksTable.updatedAt].toEpochMilliseconds()
)

private fun noteToModel(row: ResultRow, deckPublicId: Uuid) = Note(
    publicId = row[NotesTable.publicId],
    deckId = deckPublicId,
    front = row[NotesTable.front],
    back = row[NotesTable.back],
    reversible = row[NotesTable.reversible],
    createdAt = row[NotesTable.createdAt].toEpochMilliseconds(),
    updatedAt = row[NotesTable.updatedAt].toEpochMilliseconds()
)

private fun cardToModel(row: ResultRow, notePublicId: Uuid) = Card(
    publicId = row[CardsTable.publicId],
    noteId = notePublicId,
    direction = row[CardsTable.direction],
    state = row[CardsTable.state],
    learningStep = row[CardsTable.learningStep],
    stability = row[CardsTable.stability],
    difficulty = row[CardsTable.difficulty],
    due = row[CardsTable.due].toEpochMilliseconds(),
    lastReview = row[CardsTable.lastReview]?.toEpochMilliseconds(),
    updatedAt = row[CardsTable.updatedAt].toEpochMilliseconds()
)

/** [startOfToday] is when the user's day began, for counting [FlashcardData.newCardsToday]. */
suspend fun listFlashcards(userPublicId: Uuid, startOfToday: Instant): FlashcardData = withTransaction {
    val deckRows = DecksTable.selectAll()
        .where { DecksTable.userPublicId eq userPublicId }
        .orderBy(DecksTable.name)
        .toList()
    val deckIds = deckRows.associate { it[DecksTable.id] to it[DecksTable.publicId] }
    if (deckIds.isEmpty()) return@withTransaction FlashcardData(emptyList(), emptyList(), emptyList())

    val noteRows = NotesTable.selectAll()
        .where { NotesTable.deckId inList deckIds.keys }
        .orderBy(NotesTable.id)
        .toList()
    val noteIds = noteRows.associate { it[NotesTable.id] to it[NotesTable.publicId] }
    val cardRows = if (noteIds.isEmpty()) emptyList() else CardsTable.selectAll()
        .where { CardsTable.noteId inList noteIds.keys }
        .orderBy(CardsTable.id)
        .toList()

    val newCardsToday = (ReviewLogsTable innerJoin CardsTable innerJoin NotesTable innerJoin DecksTable)
        .selectAll()
        .where {
            (DecksTable.userPublicId eq userPublicId) and
                    (ReviewLogsTable.stateBefore eq CardState.NEW) and
                    (ReviewLogsTable.reviewedAt greaterEq startOfToday)
        }
        .count()

    FlashcardData(
        decks = deckRows.map(::deckToModel),
        notes = noteRows.map { noteToModel(it, deckIds.getValue(it[NotesTable.deckId])) },
        cards = cardRows.map { cardToModel(it, noteIds.getValue(it[CardsTable.noteId])) },
        newCardsToday = newCardsToday.toInt()
    )
}

private val scheduler = FsrsScheduler()

/** Reschedules the card with FSRS and logs the answer. Null when the card isn't found. */
suspend fun answerCard(userPublicId: Uuid, cardPublicId: Uuid, rating: Rating): Card? = withTransaction {
    val row = (CardsTable innerJoin NotesTable innerJoin DecksTable)
        .select(CardsTable.columns + NotesTable.publicId)
        .where { (CardsTable.publicId eq cardPublicId) and (DecksTable.userPublicId eq userPublicId) }
        .singleOrNull() ?: return@withTransaction null
    val cardId = row[CardsTable.id]
    val now = Clock.System.now()
    val (updated, outcome) = scheduler.review(cardToModel(row, row[NotesTable.publicId]), rating, now.toEpochMilliseconds())

    CardsTable.update({ CardsTable.id eq cardId }) {
        it[state] = updated.state
        it[learningStep] = updated.learningStep
        it[stability] = updated.stability
        it[difficulty] = updated.difficulty
        it[due] = Instant.fromEpochMilliseconds(updated.due)
        it[lastReview] = now
        it[updatedAt] = now
    }
    ReviewLogsTable.insert {
        it[this.cardId] = cardId
        it[this.rating] = rating
        it[reviewedAt] = now
        it[stateBefore] = outcome.stateBefore
        it[elapsedDays] = outcome.elapsedDays
        it[scheduledDays] = outcome.scheduledDays
        it[stabilityAfter] = updated.stability!!
        it[difficultyAfter] = updated.difficulty!!
    }
    updated
}

/** Creates the note's forward card, plus a reverse card when it's reversible. Null when the deck isn't found. */
suspend fun createNote(userPublicId: Uuid, deckPublicId: Uuid, params: NoteParams): NoteResult? {
    val valid = params.validated()
    return withTransaction {
        val deckId = DecksTable.select(DecksTable.id)
            .where { (DecksTable.publicId eq deckPublicId) and (DecksTable.userPublicId eq userPublicId) }
            .singleOrNull()?.get(DecksTable.id) ?: return@withTransaction null
        val now = Clock.System.now()
        val noteId = NotesTable.insertAndGetId {
            it[this.deckId] = deckId
            it[front] = valid.front
            it[back] = valid.back
            it[reversible] = valid.reversible
            it[createdAt] = now
            it[updatedAt] = now
        }
        insertCard(noteId, CardDirection.FORWARD, now)
        if (valid.reversible) insertCard(noteId, CardDirection.REVERSE, now)
        noteResult(noteId, deckPublicId)
    }
}

/**
 * Keeps existing schedules. Turning reversible on adds a new reverse card; turning it off deletes
 * that card and its review history. Null when the note isn't found.
 */
suspend fun updateNote(userPublicId: Uuid, notePublicId: Uuid, params: NoteParams): NoteResult? {
    val valid = params.validated()
    return withTransaction {
        val (noteId, deckPublicId) = findOwnedNote(userPublicId, notePublicId) ?: return@withTransaction null
        val now = Clock.System.now()
        NotesTable.update({ NotesTable.id eq noteId }) {
            it[front] = valid.front
            it[back] = valid.back
            it[reversible] = valid.reversible
            it[updatedAt] = now
        }
        val hasReverse = CardsTable.selectAll()
            .where { (CardsTable.noteId eq noteId) and (CardsTable.direction eq CardDirection.REVERSE) }
            .any()
        if (valid.reversible && !hasReverse) {
            insertCard(noteId, CardDirection.REVERSE, now)
        } else if (!valid.reversible && hasReverse) {
            CardsTable.deleteWhere { (CardsTable.noteId eq noteId) and (CardsTable.direction eq CardDirection.REVERSE) }
        }
        noteResult(noteId, deckPublicId)
    }
}

/** Deletes the note with its cards and their review history. False when the note isn't found. */
suspend fun deleteNote(userPublicId: Uuid, notePublicId: Uuid): Boolean = withTransaction {
    val (noteId, _) = findOwnedNote(userPublicId, notePublicId) ?: return@withTransaction false
    NotesTable.deleteWhere { NotesTable.id eq noteId }
    true
}

/** The note's row id and its deck's public id, when the note belongs to the user. */
private fun findOwnedNote(userPublicId: Uuid, notePublicId: Uuid) =
    (NotesTable innerJoin DecksTable)
        .select(NotesTable.id, DecksTable.publicId)
        .where { (NotesTable.publicId eq notePublicId) and (DecksTable.userPublicId eq userPublicId) }
        .singleOrNull()
        ?.let { it[NotesTable.id] to it[DecksTable.publicId] }

private fun insertCard(noteId: EntityID<Int>, direction: CardDirection, now: Instant) {
    CardsTable.insert {
        it[this.noteId] = noteId
        it[this.direction] = direction
        it[due] = now
        it[updatedAt] = now
    }
}

private fun noteResult(noteId: EntityID<Int>, deckPublicId: Uuid): NoteResult {
    val note = noteToModel(NotesTable.selectAll().where { NotesTable.id eq noteId }.single(), deckPublicId)
    val cards = CardsTable.selectAll()
        .where { CardsTable.noteId eq noteId }
        .orderBy(CardsTable.id)
        .map { cardToModel(it, note.publicId) }
    return NoteResult(note, cards)
}
