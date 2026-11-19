package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityEntry
import com.mushusax.monkeyfury.data.ActivityParams
import com.mushusax.monkeyfury.data.EntryValueParams
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.Response
import java.time.LocalDate

class NetworkActivityRepository(private val api: MonkeyFuryApi) : ActivityRepository {

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    override val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    override suspend fun refreshActivities() {
        _activities.value = api.getActivities().bodyOrThrow()
    }

    override suspend fun createActivity(params: ActivityParams): Activity {
        val created = api.createActivity(params).bodyOrThrow()
        _activities.update { it + created }
        return created
    }

    override suspend fun updateActivity(id: String, params: ActivityParams): Activity {
        val updated = api.updateActivity(id, params).bodyOrThrow()
        _activities.update { list -> list.map { if (it.publicId == id) updated else it } }
        return updated
    }

    override suspend fun archiveActivity(id: String) {
        api.archiveActivity(id).checkSuccess()
        _activities.update { list -> list.filterNot { it.publicId == id } }
    }

    override suspend fun getEntries(from: LocalDate, to: LocalDate): List<ActivityEntry> =
        api.getEntries(from.toString(), to.toString()).bodyOrThrow()

    override suspend fun saveEntry(activityId: String, date: LocalDate, value: Double): ActivityEntry =
        api.saveEntry(activityId, date.toString(), EntryValueParams(value)).bodyOrThrow()

    override suspend fun deleteEntry(activityId: String, date: LocalDate) {
        api.deleteEntry(activityId, date.toString()).checkSuccess()
    }

    override fun clear() {
        _activities.value = emptyList()
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        checkSuccess()
        return body() ?: throw ActivityApiException(code(), "Empty response from server")
    }

    private fun Response<*>.checkSuccess() {
        if (!isSuccessful) {
            val detail = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: message()
            throw ActivityApiException(code(), detail.ifBlank { "Server error ${code()}" })
        }
    }
}
