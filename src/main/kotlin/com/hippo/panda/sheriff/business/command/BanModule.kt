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

import com.hippo.panda.sheriff.base.configure
import com.hippo.panda.sheriff.util.ban
import com.hippo.panda.sheriff.util.findUsers
import com.hippo.panda.sheriff.util.isAdmin
import com.hippo.panda.sheriff.util.toMarkdown
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.*

class BanModule: CommandModule() {
    override val isAdminOnly: Boolean
        get() = true

    override val command: String
        get() = "ban"

    override fun handleCommand(bot: Bot, update: Update, message: Message, chat: Chat, user: User, text: String) {
        val toBan = message.findUsers()
        if (toBan.isEmpty()) return

        val admins = toBan.filter { it.isAdmin }
        val fellows = toBan.filter { !it.isAdmin }

        val banned = mutableListOf<User>()
        val notBanned = mutableListOf<User>()
        fellows.forEach {
            if (bot.ban(chat, it, configure.banDuration)) {
                banned.add(it)
            } else {
                notBanned.add(it)
            }
        }

        val sb = StringBuilder("${user.toMarkdown()} 发出了禁言请求。")
        if (banned.isNotEmpty()) {
            sb.append("\n${banned.joinToString("，") { it.toMarkdown() }} 被禁言 ${configure.banDuration / 60 / 1000} 分钟。")
        }
        if (notBanned.isNotEmpty()) {
            sb.append("\n${notBanned.joinToString("，") { it.toMarkdown() }} 无法被禁言。")
        }
        if (admins.isNotEmpty()) {
            sb.append("\n${admins.joinToString("，") { it.toMarkdown() }} 是管理员，无法被禁言。")
        }

        bot.sendMessage(
            chatId = chat.id,
            text = sb.toString(),
            replyToMessageId = message.messageId,
            parseMode = ParseMode.MARKDOWN
        )
    }
}
