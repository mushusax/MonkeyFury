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

enum class ActivityKind { BOOLEAN, NUMBER, DURATION }

/** An activity as the client sees it. DURATION values and goals are whole minutes. */
@Serializable
data class Activity(
    val publicId: Uuid,
    val name: String,
    val category: String,
    val kind: ActivityKind,
    val unit: String,
    val goal: Double?,
    /** True when a smaller value is the better day, e.g. alcoholic drinks; [goal] is then a limit. */
    val lowerIsBetter: Boolean,
    val sortOrder: Int
)

/** The body of POST and PUT /activities. */
@Serializable
data class ActivityParams(
    val name: String,
    val category: String = "",
    val kind: ActivityKind,
    val unit: String = "",
    val goal: Double? = null,
    val lowerIsBetter: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
data class ActivityEntry(
    val activityId: Uuid,
    val date: LocalDate,
    val value: Double
)

@Serializable
data class EntryValueParams(val value: Double)

object ActivitiesTable : IntIdTable("activities") {
    val publicId = uuid("public_id").uniqueIndex().clientDefault { Uuid.random() }
    val userPublicId = reference("user_public_id", UsersTable.publicId, onDelete = ReferenceOption.CASCADE).index()
    val name = varchar("name", 100)
    val category = varchar("category", 100).default("")
    val kind = enumerationByName<ActivityKind>("kind", 16)
    val unit = varchar("unit", 20).default("")
    val goal = double("goal").nullable()
    val lowerIsBetter = bool("lower_is_better").default(false)
    val sortOrder = integer("sort_order").default(0)
    val archived = bool("archived").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

object ActivityEntriesTable : IntIdTable("activity_entries") {
    val activityId = reference("activity_id", ActivitiesTable, onDelete = ReferenceOption.CASCADE)
    val date = date("date")
    val value = double("value")
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(activityId, date)
    }
}

class ActivityDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ActivityDAO>(ActivitiesTable)

    var publicId by ActivitiesTable.publicId
    var userPublicId by ActivitiesTable.userPublicId
    var name by ActivitiesTable.name
    var category by ActivitiesTable.category
    var kind by ActivitiesTable.kind
    var unit by ActivitiesTable.unit
    var goal by ActivitiesTable.goal
    var lowerIsBetter by ActivitiesTable.lowerIsBetter
    var sortOrder by ActivitiesTable.sortOrder
    var archived by ActivitiesTable.archived
    val createdAt by ActivitiesTable.createdAt
}

fun daoToModel(dao: ActivityDAO) = Activity(
    publicId = dao.publicId,
    name = dao.name,
    category = dao.category,
    kind = dao.kind,
    unit = dao.unit,
    goal = dao.goal,
    lowerIsBetter = dao.lowerIsBetter,
    sortOrder = dao.sortOrder
)

/** Throws IllegalArgumentException (answered with 400) when the params are unusable. */
private fun ActivityParams.validated(): ActivityParams {
    val name = name.trim()
    require(name.isNotEmpty()) { "Activity name is required" }
    require(name.length <= 100) { "Activity name is too long" }
    require(category.trim().length <= 100) { "Category is too long" }
    require(unit.trim().length <= 20) { "Unit is too long" }
    require(goal == null || goal > 0) { "Goal must be positive" }
    return copy(
        name = name,
        category = category.trim(),
        // Only NUMBER activities have a unit; BOOLEAN has nothing to scale, so no goal either
        unit = if (kind == ActivityKind.NUMBER) unit.trim() else "",
        goal = if (kind == ActivityKind.BOOLEAN) null else goal
    )
}

private fun findOwned(userPublicId: Uuid, activityId: Uuid): ActivityDAO? =
    ActivityDAO.find {
        (ActivitiesTable.publicId eq activityId) and
                (ActivitiesTable.userPublicId eq userPublicId) and
                (ActivitiesTable.archived eq false)
    }.singleOrNull()

suspend fun listActivities(userPublicId: Uuid): List<Activity> = withTransaction {
    ActivityDAO.find { (ActivitiesTable.userPublicId eq userPublicId) and (ActivitiesTable.archived eq false) }
        .sortedWith(compareBy({ it.category.lowercase() }, { it.sortOrder }, { it.name.lowercase() }))
        .map(::daoToModel)
}

suspend fun createActivity(userPublicId: Uuid, params: ActivityParams): Activity {
    val valid = params.validated()
    return withTransaction {
        daoToModel(ActivityDAO.new {
            this.userPublicId = userPublicId
            name = valid.name
            category = valid.category
            kind = valid.kind
            unit = valid.unit
            goal = valid.goal
            lowerIsBetter = valid.lowerIsBetter
            sortOrder = valid.sortOrder
        })
    }
}

/** Returns null when the activity doesn't exist or belongs to someone else. */
suspend fun updateActivity(userPublicId: Uuid, activityId: Uuid, params: ActivityParams): Activity? {
    val valid = params.validated()
    return withTransaction {
        val dao = findOwned(userPublicId, activityId) ?: return@withTransaction null
        dao.name = valid.name
        dao.category = valid.category
        dao.kind = valid.kind
        dao.unit = valid.unit
        dao.goal = valid.goal
        dao.lowerIsBetter = valid.lowerIsBetter
        dao.sortOrder = valid.sortOrder
        daoToModel(dao)
    }
}

/** Archives rather than deletes, so history survives. Returns false when not found. */
suspend fun archiveActivity(userPublicId: Uuid, activityId: Uuid): Boolean = withTransaction {
    val dao = findOwned(userPublicId, activityId) ?: return@withTransaction false
    dao.archived = true
    true
}

suspend fun listEntries(userPublicId: Uuid, from: LocalDate, to: LocalDate): List<ActivityEntry> = withTransaction {
    val owned = ActivityDAO.find {
        (ActivitiesTable.userPublicId eq userPublicId) and (ActivitiesTable.archived eq false)
    }.associateBy({ it.id.value }, { it.publicId })
    if (owned.isEmpty()) return@withTransaction emptyList()

    ActivityEntriesTable.selectAll()
        .where {
            (ActivityEntriesTable.activityId inList owned.keys.map { EntityID(it, ActivitiesTable) }) and
                    (ActivityEntriesTable.date.between(from, to))
        }
        .map {
            ActivityEntry(
                activityId = owned.getValue(it[ActivityEntriesTable.activityId].value),
                date = it[ActivityEntriesTable.date],
                value = it[ActivityEntriesTable.value]
            )
        }
}

/** Creates or replaces the entry for that day. Returns null when the activity isn't found. */
suspend fun saveEntry(userPublicId: Uuid, activityId: Uuid, date: LocalDate, value: Double): ActivityEntry? {
    require(value.isFinite() && value >= 0) { "Value must be zero or more" }
    return withTransaction {
        val dao = findOwned(userPublicId, activityId) ?: return@withTransaction null
        val stored = when (dao.kind) {
            ActivityKind.BOOLEAN -> if (value > 0) 1.0 else 0.0
            ActivityKind.DURATION -> Math.round(value).toDouble()
            ActivityKind.NUMBER -> value
        }
        ActivityEntriesTable.upsert(
            ActivityEntriesTable.activityId, ActivityEntriesTable.date,
            onUpdateExclude = listOf(ActivityEntriesTable.id)
        ) {
            it[this.activityId] = dao.id
            it[this.date] = date
            it[this.value] = stored
            it[updatedAt] = Clock.System.now()
        }
        ActivityEntry(activityId, date, stored)
    }
}

/** Returns false when the activity isn't found; clearing a day with no entry still succeeds. */
suspend fun deleteEntry(userPublicId: Uuid, activityId: Uuid, date: LocalDate): Boolean = withTransaction {
    val dao = findOwned(userPublicId, activityId) ?: return@withTransaction false
    ActivityEntriesTable.deleteWhere {
        (ActivityEntriesTable.activityId eq dao.id) and (ActivityEntriesTable.date eq date)
    }
    true
}
