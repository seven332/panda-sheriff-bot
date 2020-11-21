/*
 * Copyright 2020 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.panda.sheriff.base

import me.ivmg.telegram.entities.ChatMember
import me.ivmg.telegram.entities.User
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

data class Recaptcha(
    val id: Int,
    val chatId: Long,
    val userId: Long,
    val messageId: Long = 0,
    val jobId: Int = 0,
    val life: Long
) {
    fun duplicate(messageId: Long, jobId: Int): Recaptcha {
        return Recaptcha(
            id = id,
            chatId = chatId,
            userId = userId,
            messageId = messageId,
            jobId = jobId,
            life = life
        )
    }
}

interface Store {
    fun addUser(user: User)
    fun removeUser(user: User)
    fun findUser(userId: Long): User?
    fun findUser(username: String): User?

    fun setAdmins(admins: List<ChatMember>)
    fun isAdmin(user: User): Boolean

    fun createRecaptcha(recaptcha: Recaptcha): Int
    fun updateRecaptcha(id: Int, recaptcha: Recaptcha)
    fun deleteRecaptcha(id: Int)
    fun findRecaptcha(id: Int): Recaptcha?
    fun getAllRecaptchas(): List<Recaptcha>
}

private object UserTable: IntIdTable() {
    val userId = long("user_id")
    val isBot = bool("is_bot")
    val firstName = text("first_name")
    val lastName = text("last_name").nullable()
    val username = text("username").nullable()
    val languageCode = text("language_code").nullable()
}

class UserEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<UserEntry>(UserTable)

    var userId by UserTable.userId
    var isBot by UserTable.isBot
    var firstName by UserTable.firstName
    var lastName by UserTable.lastName
    var username by UserTable.username
    var languageCode by UserTable.languageCode

    fun toUser(): User {
        return User(
            id = userId,
            isBot = isBot,
            firstName = firstName,
            lastName = lastName,
            username = username,
            languageCode = languageCode
        )
    }
}

private object RecaptchaTable: IntIdTable() {
    val chatId = long("chat_id")
    val userId = long("user_id")
    val messageId = long("message_id")
    val jobId = integer("job_id")
    val expired = long("expired")
}

class RecaptchaEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RecaptchaEntry>(RecaptchaTable)

    var chatId by RecaptchaTable.chatId
    var userId by RecaptchaTable.userId
    var messageId by RecaptchaTable.messageId
    var jobId by RecaptchaTable.jobId
    var expired by RecaptchaTable.expired

    fun toRecaptcha(): Recaptcha? {
        if (!isValid()) return null
        return Recaptcha(
            id = id.value,
            chatId = chatId,
            userId = userId,
            messageId = messageId,
            jobId = jobId,
            life = expired - System.currentTimeMillis()
        )
    }

    private fun isValid(): Boolean = chatId != 0L &&
            userId != 0L &&
            messageId != 0L &&
            expired != 0L
}

class RealStore: Store {
    private val db: Database = Database.connect(configure.dbPath, driver = "org.sqlite.JDBC").also {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(it) {
            SchemaUtils.create(UserTable)
            SchemaUtils.create(RecaptchaTable)
        }
    }

    private var admins: List<ChatMember> = emptyList()

    override fun addUser(user: User) {
        transaction(db) {
            val old = UserEntry.find { UserTable.userId eq user.id }.firstOrNull()
            if (old != null) {
                // Update
                old.isBot = user.isBot
                old.firstName = user.firstName
                old.lastName = user.lastName
                old.username = user.username
                old.languageCode = user.languageCode
            } else {
                // New
                UserEntry.new {
                    userId = user.id
                    isBot = user.isBot
                    firstName = user.firstName
                    lastName = user.lastName
                    username = user.username
                    languageCode = user.languageCode
                }
            }
        }
    }

    override fun removeUser(user: User) {
        transaction(db) {
            UserEntry.find { UserTable.userId eq user.id }.forEach { it.delete() }
        }
    }

    override fun findUser(userId: Long): User? {
        return transaction(db) {
            UserEntry.find { UserTable.userId eq userId }.firstOrNull()?.toUser()
        }
    }

    override fun findUser(username: String): User? {
        return transaction(db) {
            UserEntry.find { UserTable.username.lowerCase() eq username.toLowerCase() }.firstOrNull()?.toUser()
        }
    }

    override fun setAdmins(admins: List<ChatMember>) {
        this.admins = admins
    }

    override fun isAdmin(user: User): Boolean {
        return admins.any { it.user.id == user.id }
    }

    override fun createRecaptcha(recaptcha: Recaptcha): Int {
        val record = transaction(db) {
            RecaptchaEntry.new {
                this.chatId = recaptcha.chatId
                this.userId = recaptcha.userId
                this.messageId = recaptcha.messageId
                this.jobId = recaptcha.jobId
                this.expired = System.currentTimeMillis() + recaptcha.life
            }
        }
        return record.id.value
    }

    override fun updateRecaptcha(id: Int, recaptcha: Recaptcha) {
        transaction(db) {
            val entry = RecaptchaEntry.find { RecaptchaTable.id eq id }.firstOrNull() ?: return@transaction
            entry.chatId = recaptcha.chatId
            entry.userId = recaptcha.userId
            entry.messageId = recaptcha.messageId
            entry.jobId = recaptcha.jobId
            entry.expired = System.currentTimeMillis() + recaptcha.life
        }
    }

    override fun deleteRecaptcha(id: Int) {
        transaction(db) {
            RecaptchaEntry.find { RecaptchaTable.id eq id }.forEach { it.delete() }
        }
    }

    override fun findRecaptcha(id: Int): Recaptcha? {
        return transaction(db) {
            RecaptchaEntry
                .find { RecaptchaTable.id eq id }
                .firstOrNull()
                .let { it?.toRecaptcha() }
        }
    }

    override fun getAllRecaptchas(): List<Recaptcha> {
        return transaction(db) {
            RecaptchaEntry.all().mapNotNull { it.toRecaptcha() }
        }
    }
}
