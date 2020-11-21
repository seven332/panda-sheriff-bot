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

package com.hippo.panda.sheriff.util

import com.hippo.panda.sheriff.base.store
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.Chat
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.User
import java.util.*

val User.isAdmin: Boolean
    get() = store.isAdmin(this)

val User.displayName: String
    get() = if (lastName != null) "$firstName $lastName" else firstName

fun User.toMarkdown(): String {
    val username = username
    return if (username != null) {
        "@" + username.replace("_", "\\_")
    } else {
        "[$displayName](tg://user?id=$id)"
    }
}

/**
 * Finds all mentioned users in message.
 */
fun Message.findUsers(): List<User> {
    val text = this.text ?: return emptyList()
    val entities = this.entities?.filter { it.type == "text_mention" || it.type == "mention" } ?: return emptyList()
    if (entities.isEmpty()) return emptyList()

    val users = mutableListOf<User>()
    entities.forEach { entity ->
        var user = entity.user
        if (user != null) {
            users.add(user)
        } else {
            val username = text.substring(entity.offset, entity.offset + entity.length)
            user = store.findUser(username.let { if (it.startsWith("@")) it.substring(1) else it })
            if (user != null) {
                users.add(user)
            }
        }
    }

    return users
}

fun Bot.ban(chat: Chat, user: User, duration: Long = 0): Boolean {
    return restrictChatMember(
        chat.id,
        user.id,
        Date(if (duration == 0L) 0 else System.currentTimeMillis() + duration),
        canSendMessages = false,
        canSendMediaMessages = false,
        canSendOtherMessages = false,
        canAddWebPagePreviews = false
    ).first?.body()?.result == true
}

fun Bot.unban(chat: Chat, user: User): Boolean {
    return unban(chat.id, user.id)
}

fun Bot.unban(chatId: Long, userId: Long): Boolean {
    return restrictChatMember(
        chatId,
        userId,
        Date(0),
        canSendMessages = true,
        canSendMediaMessages = true,
        canSendOtherMessages = true,
        canAddWebPagePreviews = true
    ).first?.body()?.result == true
}
