package com.mushusax.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelSuspendTransaction
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

@Serializable
data class User(
    val publicId: Uuid,
    val email: String,
    val firstName: String,
    val lastName: String
)

object UsersTable : IntIdTable("users") {
    val publicId = uuid("public_id").uniqueIndex().clientDefault { Uuid.random() }
    val googleSub = varchar("google_sub", 255).uniqueIndex()
    val email = varchar("email", 320)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100).default("")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    /** Stamped into each session JWT; logging out bumps it, which invalidates every older token. */
    val sessionVersion = integer("session_version").default(0)
}

class UserDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserDAO>(UsersTable)

    var publicId by UsersTable.publicId
    var googleSub by UsersTable.googleSub
    var email by UsersTable.email
    var firstName by UsersTable.firstName
    var lastName by UsersTable.lastName
    var sessionVersion by UsersTable.sessionVersion
    val createdAt by UsersTable.createdAt
}

suspend fun <T> withTransaction(block: suspend JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) {
    inTopLevelSuspendTransaction { block() }
}

private val log = LoggerFactory.getLogger("users")

/** A user plus the session version their next JWT must carry. */
data class LoginUser(val user: User, val sessionVersion: Int)

suspend fun getUser(googleSub: String, email: String, firstName: String, lastName: String): LoginUser {
    var created = false
    val login = withTransaction {
        val existing = UserDAO.find { UsersTable.googleSub eq googleSub }.singleOrNull()
        val dao = existing ?: UserDAO.new {
            this.googleSub = googleSub
            this.email = email
            this.firstName = firstName
            this.lastName = lastName
        }.also { created = true }
        LoginUser(daoToModel(dao), dao.sessionVersion)
    }
    if (created) log.info("Inserted new user {}", login.user.publicId)
    return login
}

/** The user's current session version, or null when the user no longer exists. */
suspend fun sessionVersion(publicId: Uuid): Int? = withTransaction {
    UsersTable.select(UsersTable.sessionVersion)
        .where { UsersTable.publicId eq publicId }
        .singleOrNull()
        ?.get(UsersTable.sessionVersion)
}

/** Invalidates every session JWT issued to this user so far, on all devices. */
suspend fun endSessions(publicId: Uuid) = withTransaction {
    UsersTable.update({ UsersTable.publicId eq publicId }) {
        it[sessionVersion] = sessionVersion + 1
    }
}

fun daoToModel(dao: UserDAO) = User(
    publicId = dao.publicId,
    email = dao.email,
    firstName = dao.firstName,
    lastName = dao.lastName
)
