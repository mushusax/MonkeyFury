package com.mushusax.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelSuspendTransaction

@Serializable
data class User(
    val username: String,
    val email: String,
    val name: String
)

object UsersTable : IntIdTable("users") {
    val username = varchar("username", 255)
    val email = varchar("email", 255)
    val name = varchar("name", 255)
}

class UserDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserDAO>(UsersTable)

    var username by UsersTable.username
    var email by UsersTable.email
    var name by UsersTable.name

    override fun toString(): String {
        return "Task(id=$id, username=$username, email=$email, name=$name)"
    }
}

suspend fun <T> withTransaction(block: suspend JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) {
    inTopLevelSuspendTransaction { block() }
}

fun daoToModel(dao: UserDAO) = User(
    dao.username,
    dao.email,
    dao.name
)