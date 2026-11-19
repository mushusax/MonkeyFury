package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityEntry
import com.mushusax.monkeyfury.data.ActivityParams
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Activities and their daily entries. Every call throws [java.io.IOException] when the server
 * can't be reached and [ActivityApiException] when it answers with an error.
 */
interface ActivityRepository {
    /** The latest activity list, shared by every screen; updated by [refreshActivities] and edits. */
    val activities: StateFlow<List<Activity>>

    suspend fun refreshActivities()
    suspend fun createActivity(params: ActivityParams): Activity
    suspend fun updateActivity(id: String, params: ActivityParams): Activity
    suspend fun archiveActivity(id: String)

    /** Entries from [from] through [to], inclusive. */
    suspend fun getEntries(from: LocalDate, to: LocalDate): List<ActivityEntry>
    suspend fun saveEntry(activityId: String, date: LocalDate, value: Double): ActivityEntry
    suspend fun deleteEntry(activityId: String, date: LocalDate)

    /** Forgets cached activities, e.g. on logout, so the next user never sees them. */
    fun clear()
}

class ActivityApiException(val code: Int, message: String) : Exception(message)

/** A short message for showing a failed repository call to the user. */
fun Throwable.toUserMessage(): String = when (this) {
    is java.io.IOException -> "Couldn't reach the server"
    is ActivityApiException -> if (code == 401) "Your session expired, please sign in again" else message ?: "Server error"
    else -> message ?: "Something went wrong"
}
