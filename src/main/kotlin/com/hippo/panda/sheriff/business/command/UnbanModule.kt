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

package com.hippo.panda.sheriff.business.command

import com.hippo.panda.sheriff.util.findUsers
import com.hippo.panda.sheriff.util.isAdmin
import com.hippo.panda.sheriff.util.toMarkdown
import com.hippo.panda.sheriff.util.unban
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.*

class UnbanModule: CommandModule() {
    override val isAdminOnly: Boolean
        get() = true

    override val command: String
        get() = "unban"

    override fun handleCommand(bot: Bot, update: Update, message: Message, chat: Chat, user: User, text: String) {
        val toBan = message.findUsers()
        if (toBan.isEmpty()) return

        val admins = toBan.filter { it.isAdmin }
        val fellows = toBan.filter { !it.isAdmin }

        val unbanned = mutableListOf<User>()
        val notUnbanned = mutableListOf<User>()
        fellows.forEach {
            if (bot.unban(chat, it)) {
                unbanned.add(it)
            } else {
                notUnbanned.add(it)
            }
        }

        val sb = StringBuilder("${user.toMarkdown()} 发出了取消禁言请求。")
        if (unbanned.isNotEmpty()) {
            sb.append("\n${unbanned.joinToString("，") { it.toMarkdown() }} 被取消禁言。")
        }
        if (notUnbanned.isNotEmpty()) {
            sb.append("\n${notUnbanned.joinToString("，") { it.toMarkdown() }} 无法被取消禁言。")
        }
        if (admins.isNotEmpty()) {
            sb.append("\n${admins.joinToString("，") { it.toMarkdown() }} 是管理员，无法被取消禁言。")
        }

        bot.sendMessage(
            chatId = chat.id,
            text = sb.toString(),
            replyToMessageId = message.messageId,
            parseMode = ParseMode.MARKDOWN
        )
    }
}
