package com.mushusax.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
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
}

class UserDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserDAO>(UsersTable)

    var publicId by UsersTable.publicId
    var googleSub by UsersTable.googleSub
    var email by UsersTable.email
    var firstName by UsersTable.firstName
    var lastName by UsersTable.lastName
}

suspend fun <T> withTransaction(block: suspend JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) {
    inTopLevelSuspendTransaction { block() }
}

private val log = LoggerFactory.getLogger("users")

suspend fun getUser(googleSub: String, email: String, firstName: String, lastName: String): User {
    var created = false
    val user = withTransaction {
        val existing = UserDAO.find { UsersTable.googleSub eq googleSub }.singleOrNull()
        val dao = existing ?: UserDAO.new {
            this.googleSub = googleSub
            this.email = email
            this.firstName = firstName
            this.lastName = lastName
        }.also { created = true }
        daoToModel(dao)
    }
    if (created) log.info("Inserted new user {}", user.publicId)
    return user
}

fun daoToModel(dao: UserDAO) = User(
    publicId = dao.publicId,
    email = dao.email,
    firstName = dao.firstName,
    lastName = dao.lastName
)
