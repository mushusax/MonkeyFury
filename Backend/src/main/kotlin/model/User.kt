package com.mushusax.model

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object Users : IntIdTable("users") {
    val username = varchar("username", 255)
    val email = varchar("email", 255)
    val name = varchar("name", 255)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var email by Users.email
    var name by Users.name

    override fun toString(): String {
        return "Task(id=$id, username=$username, email=$email, name=$name)"
    }
}