package com.mushusax.monkeyfury.data

import kotlinx.serialization.Serializable

/** BOOK progress is pages, ANIME progress is episodes, and a MOVIE has no progress, only a status. */
enum class MediaKind(val label: String, val pluralLabel: String) {
    BOOK("Book", "Books"),
    ANIME("Anime", "Anime"),
    MOVIE("Movie", "Movies")
}

/** Declared in the order the library lists its sections. */
enum class MediaStatus(val label: String) {
    IN_PROGRESS("In Progress"),
    PLANNED("Planned"),
    ON_HOLD("On Hold"),
    COMPLETED("Completed"),
    DROPPED("Dropped")
}

/** A book, anime, or movie. Dates are yyyy-MM-dd. */
@Serializable
data class Media(
    val publicId: String,
    val kind: MediaKind,
    val title: String,
    /** Author, studio, or director. */
    val creator: String = "",
    val status: MediaStatus,
    val progress: Int = 0,
    val total: Int? = null,
    /** Null until rated. */
    val thumbsUp: Boolean? = null,
    val startedOn: String? = null,
    val finishedOn: String? = null,
    val notes: String = ""
)

/** The body of POST and PUT /media. Dates are yyyy-MM-dd. */
@Serializable
data class MediaParams(
    val kind: MediaKind,
    val title: String,
    val creator: String,
    val status: MediaStatus,
    val progress: Int,
    val total: Int?,
    val thumbsUp: Boolean?,
    val startedOn: String?,
    val finishedOn: String?,
    val notes: String
)

/** The body of POST /media/{id}/progress. A negative [amount] undoes progress. [date] is yyyy-MM-dd. */
@Serializable
data class ProgressParams(val amount: Int, val date: String)

/** Pages or episodes for one item on one day; a finished movie counts as 1. [date] is yyyy-MM-dd. */
@Serializable
data class MediaLogEntry(
    val mediaId: String,
    val date: String,
    val amount: Int
)

/** The edit dialog starts from these. */
fun Media.toParams() = MediaParams(
    kind = kind,
    title = title,
    creator = creator,
    status = status,
    progress = progress,
    total = total,
    thumbsUp = thumbsUp,
    startedOn = startedOn,
    finishedOn = finishedOn,
    notes = notes
)

val MediaKind.hasProgress: Boolean
    get() = this != MediaKind.MOVIE

/** What the creator field is called for this kind, e.g. "Author". */
val MediaKind.creatorLabel: String
    get() = when (this) {
        MediaKind.BOOK -> "Author"
        MediaKind.ANIME -> "Studio"
        MediaKind.MOVIE -> "Director"
    }

/** What progress counts for this kind, e.g. "Pages"; empty for a movie. */
val MediaKind.progressLabel: String
    get() = when (this) {
        MediaKind.BOOK -> "Pages"
        MediaKind.ANIME -> "Episodes"
        MediaKind.MOVIE -> ""
    }

/** How progress reads, e.g. "p. 142 / 380", "Ep 7", or "" for a movie. */
fun Media.formatProgress(): String {
    val prefix = when (kind) {
        MediaKind.BOOK -> "p. "
        MediaKind.ANIME -> "Ep "
        MediaKind.MOVIE -> return ""
    }
    return if (total == null) "$prefix$progress" else "$prefix$progress / $total"
}

/** Share of the total done, 0 to 1; null when there's no total to measure against. */
val Media.progressFraction: Float?
    get() = total?.takeIf { kind.hasProgress && it > 0 }?.let { (progress.toFloat() / it).coerceIn(0f, 1f) }
