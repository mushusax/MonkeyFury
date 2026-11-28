package com.mushusax.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** BOOK progress is pages, ANIME progress is episodes, and a MOVIE has no progress, only a status. */
enum class MediaKind { BOOK, ANIME, MOVIE }

enum class MediaStatus { PLANNED, IN_PROGRESS, ON_HOLD, COMPLETED, DROPPED }

/** A book, anime, or movie as the client sees it. */
@Serializable
data class Media(
    val publicId: Uuid,
    val kind: MediaKind,
    val title: String,
    /** Author, studio, or director. */
    val creator: String,
    val status: MediaStatus,
    val progress: Int,
    val total: Int?,
    /** Null until rated. */
    val thumbsUp: Boolean?,
    val startedOn: LocalDate?,
    val finishedOn: LocalDate?,
    val notes: String
)

/** The body of POST and PUT /media. Dates come from the client, which knows the user's local day. */
@Serializable
data class MediaParams(
    val kind: MediaKind,
    val title: String,
    val creator: String = "",
    val status: MediaStatus = MediaStatus.PLANNED,
    val progress: Int = 0,
    val total: Int? = null,
    val thumbsUp: Boolean? = null,
    val startedOn: LocalDate? = null,
    val finishedOn: LocalDate? = null,
    val notes: String = ""
)

/** The body of POST /media/{id}/progress. A negative [amount] undoes progress logged by mistake. */
@Serializable
data class ProgressParams(val amount: Int, val date: LocalDate)

/** Pages or episodes for one item on one day; a finished movie counts as 1. */
@Serializable
data class MediaLogEntry(
    val mediaId: Uuid,
    val date: LocalDate,
    val amount: Int
)

private const val MAX_TITLE = 200
private const val MAX_NOTES = 5000
private const val MAX_TOTAL = 100_000

object MediaTable : IntIdTable("media") {
    val publicId = uuid("public_id").uniqueIndex().clientDefault { Uuid.random() }
    val userPublicId = reference("user_public_id", UsersTable.publicId, onDelete = ReferenceOption.CASCADE).index()
    val kind = enumerationByName<MediaKind>("kind", 16)
    val title = varchar("title", MAX_TITLE)
    val creator = varchar("creator", MAX_TITLE).default("")
    val status = enumerationByName<MediaStatus>("status", 16)
    val progress = integer("progress").default(0)
    val total = integer("total").nullable()
    val thumbsUp = bool("thumbs_up").nullable()
    val startedOn = date("started_on").nullable()
    val finishedOn = date("finished_on").nullable()
    val notes = text("notes").default("")
    val archived = bool("archived").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

object MediaProgressLogTable : IntIdTable("media_progress_log") {
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val date = date("date")
    val amount = integer("amount")

    init {
        uniqueIndex(mediaId, date)
    }
}

class MediaDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MediaDAO>(MediaTable)

    var publicId by MediaTable.publicId
    var userPublicId by MediaTable.userPublicId
    var kind by MediaTable.kind
    var title by MediaTable.title
    var creator by MediaTable.creator
    var status by MediaTable.status
    var progress by MediaTable.progress
    var total by MediaTable.total
    var thumbsUp by MediaTable.thumbsUp
    var startedOn by MediaTable.startedOn
    var finishedOn by MediaTable.finishedOn
    var notes by MediaTable.notes
    var archived by MediaTable.archived
    val createdAt by MediaTable.createdAt
    var updatedAt by MediaTable.updatedAt
}

fun daoToModel(dao: MediaDAO) = Media(
    publicId = dao.publicId,
    kind = dao.kind,
    title = dao.title,
    creator = dao.creator,
    status = dao.status,
    progress = dao.progress,
    total = dao.total,
    thumbsUp = dao.thumbsUp,
    startedOn = dao.startedOn,
    finishedOn = dao.finishedOn,
    notes = dao.notes
)

/** Throws IllegalArgumentException (answered with 400) when the params are unusable. */
private fun MediaParams.validated(): MediaParams {
    val title = title.trim()
    require(title.isNotEmpty()) { "Title is required" }
    require(title.length <= MAX_TITLE) { "Title is too long" }
    require(creator.trim().length <= MAX_TITLE) { "Creator is too long" }
    require(notes.length <= MAX_NOTES) { "Notes are too long" }
    require(startedOn == null || finishedOn == null || startedOn <= finishedOn) { "Finished before it was started" }
    if (kind == MediaKind.MOVIE) {
        return copy(title = title, creator = creator.trim(), progress = 0, total = null)
    }
    require(total == null || total in 1..MAX_TOTAL) { "Total must be between 1 and $MAX_TOTAL" }
    require(progress >= 0) { "Progress can't be negative" }
    require(total == null || progress <= total) { "Progress is past the total" }
    return copy(
        title = title,
        creator = creator.trim(),
        // Completing something means all of it was read or watched
        progress = if (status == MediaStatus.COMPLETED && total != null) total else progress
    )
}

private fun findOwned(userPublicId: Uuid, mediaId: Uuid): MediaDAO? =
    MediaDAO.find {
        (MediaTable.publicId eq mediaId) and
                (MediaTable.userPublicId eq userPublicId) and
                (MediaTable.archived eq false)
    }.singleOrNull()

private fun MediaDAO.setFrom(valid: MediaParams) {
    kind = valid.kind
    title = valid.title
    creator = valid.creator
    status = valid.status
    progress = valid.progress
    total = valid.total
    thumbsUp = valid.thumbsUp
    startedOn = valid.startedOn
    finishedOn = valid.finishedOn
    notes = valid.notes
    updatedAt = Clock.System.now()
}

/** What the day's log row holds for [dao], 0 when there's no row. */
private fun loggedOn(dao: MediaDAO, date: LocalDate): Int =
    MediaProgressLogTable.selectAll()
        .where { (MediaProgressLogTable.mediaId eq dao.id) and (MediaProgressLogTable.date eq date) }
        .singleOrNull()?.get(MediaProgressLogTable.amount) ?: 0

/** Adds [amount] to the day's log row, removing the row when nothing is left in it. */
private fun addToLog(dao: MediaDAO, date: LocalDate, amount: Int) {
    if (amount == 0) return
    val newAmount = loggedOn(dao, date) + amount
    if (newAmount <= 0) {
        MediaProgressLogTable.deleteWhere {
            (MediaProgressLogTable.mediaId eq dao.id) and (MediaProgressLogTable.date eq date)
        }
    } else {
        MediaProgressLogTable.upsert(
            MediaProgressLogTable.mediaId, MediaProgressLogTable.date,
            onUpdateExclude = listOf(MediaProgressLogTable.id)
        ) {
            it[mediaId] = dao.id
            it[this.date] = date
            it[this.amount] = newAmount
        }
    }
}

/** A movie's heat map day is the day it was finished, so keep exactly one row there while it's completed. */
private fun syncMovieLog(dao: MediaDAO) {
    if (dao.kind != MediaKind.MOVIE) return
    MediaProgressLogTable.deleteWhere { MediaProgressLogTable.mediaId eq dao.id }
    val finished = dao.finishedOn
    if (dao.status == MediaStatus.COMPLETED && finished != null) addToLog(dao, finished, 1)
}

/** Most recently touched first; the client groups by kind and status. */
suspend fun listMedia(userPublicId: Uuid): List<Media> = withTransaction {
    MediaDAO.find { (MediaTable.userPublicId eq userPublicId) and (MediaTable.archived eq false) }
        .sortedByDescending { it.updatedAt }
        .map(::daoToModel)
}

suspend fun createMedia(userPublicId: Uuid, params: MediaParams): Media {
    val valid = params.validated()
    return withTransaction {
        val dao = MediaDAO.new {
            this.userPublicId = userPublicId
            setFrom(valid)
        }
        syncMovieLog(dao)
        daoToModel(dao)
    }
}

/**
 * Hand edits set progress directly without writing to the log, so fixing a count doesn't invent history.
 * Returns null when the item doesn't exist or belongs to someone else.
 */
suspend fun updateMedia(userPublicId: Uuid, mediaId: Uuid, params: MediaParams): Media? {
    val valid = params.validated()
    return withTransaction {
        val dao = findOwned(userPublicId, mediaId) ?: return@withTransaction null
        val wasMovie = dao.kind == MediaKind.MOVIE
        dao.setFrom(valid)
        // A movie's finished-day row has no meaning once it's a book or anime
        if (wasMovie && dao.kind != MediaKind.MOVIE) {
            MediaProgressLogTable.deleteWhere { MediaProgressLogTable.mediaId eq dao.id }
        }
        syncMovieLog(dao)
        daoToModel(dao)
    }
}

/**
 * Logs pages or episodes on [ProgressParams.date] and moves the status along: logging on something
 * Planned or On Hold starts it, reaching the total completes it, and undoing below the total reopens it.
 * A negative amount takes back at most what was logged on that day. Returns null when the item isn't found.
 */
suspend fun logProgress(userPublicId: Uuid, mediaId: Uuid, params: ProgressParams): Media? {
    require(params.amount != 0 && params.amount in -MAX_TOTAL..MAX_TOTAL) { "Amount must be a nonzero count" }
    return withTransaction {
        val dao = findOwned(userPublicId, mediaId) ?: return@withTransaction null
        require(dao.kind != MediaKind.MOVIE) { "Movies have no progress; mark them completed instead" }

        // Undoing only takes back what was logged that day, so progress and the heat map stay in step
        val amount = if (params.amount < 0) maxOf(params.amount, -loggedOn(dao, params.date)) else params.amount
        val before = dao.progress
        val after = (before + amount).coerceIn(0, dao.total ?: Int.MAX_VALUE)
        if (after == before) return@withTransaction daoToModel(dao)
        dao.progress = after
        addToLog(dao, params.date, after - before)

        val total = dao.total
        when {
            total != null && after == total -> {
                dao.status = MediaStatus.COMPLETED
                if (dao.finishedOn == null) dao.finishedOn = params.date
            }
            dao.status == MediaStatus.COMPLETED && params.amount < 0 -> {
                dao.status = MediaStatus.IN_PROGRESS
                dao.finishedOn = null
            }
            params.amount > 0 && (dao.status == MediaStatus.PLANNED || dao.status == MediaStatus.ON_HOLD) ->
                dao.status = MediaStatus.IN_PROGRESS
        }
        if (params.amount > 0 && dao.startedOn == null) dao.startedOn = params.date
        dao.updatedAt = Clock.System.now()
        daoToModel(dao)
    }
}

/** Archives rather than deletes, so the heat map history survives. Returns false when not found. */
suspend fun archiveMedia(userPublicId: Uuid, mediaId: Uuid): Boolean = withTransaction {
    val dao = findOwned(userPublicId, mediaId) ?: return@withTransaction false
    dao.archived = true
    true
}

suspend fun listMediaLog(userPublicId: Uuid, from: LocalDate, to: LocalDate): List<MediaLogEntry> = withTransaction {
    val owned = MediaDAO.find {
        (MediaTable.userPublicId eq userPublicId) and (MediaTable.archived eq false)
    }.associateBy({ it.id.value }, { it.publicId })
    if (owned.isEmpty()) return@withTransaction emptyList()

    MediaProgressLogTable.selectAll()
        .where {
            (MediaProgressLogTable.mediaId inList owned.keys.map { EntityID(it, MediaTable) }) and
                    (MediaProgressLogTable.date.between(from, to))
        }
        .map {
            MediaLogEntry(
                mediaId = owned.getValue(it[MediaProgressLogTable.mediaId].value),
                date = it[MediaProgressLogTable.date],
                amount = it[MediaProgressLogTable.amount]
            )
        }
}
