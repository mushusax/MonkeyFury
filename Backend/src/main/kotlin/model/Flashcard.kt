package com.mushusax.model

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.Uuid

// FSRS flashcards: a deck holds notes (what you write); each note is studied as one card per
// direction, and every answer to a card is kept as a review log. Deleting a deck cascades to all of it.

enum class CardDirection { FORWARD, REVERSE }

enum class CardState { NEW, LEARNING, REVIEW, RELEARNING }

enum class Rating { AGAIN, HARD, GOOD, EASY }

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
