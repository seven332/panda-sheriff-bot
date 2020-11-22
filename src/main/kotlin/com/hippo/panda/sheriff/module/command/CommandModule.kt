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

package com.hippo.panda.sheriff.module.command

import com.hippo.panda.sheriff.base.Module
import com.hippo.panda.sheriff.base.Register
import com.hippo.panda.sheriff.util.isAdmin
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.Chat
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.Update
import me.ivmg.telegram.entities.User

val commandDescriptions = linkedMapOf<Class<CommandModule>, String>()

abstract class CommandModule: Module {
    protected abstract val isAdminOnly: Boolean
    protected abstract val command: String
    protected abstract val description: String

    override fun willCreateBot(register: Register) {
        register.register(::checkCommand, ::handleCommand)
    }

    override fun didCreateBot(bot: Bot) {
        commandDescriptions[javaClass] = description
    }

    private fun checkCommand(update: Update): Boolean {
        val text = update.message?.text ?: return false
        return (text.startsWith("/$command ") || text == "/$command") &&
                (!isAdminOnly || update.message?.from?.isAdmin ?: false)
    }

    private fun handleCommand(bot: Bot, update: Update) {
        val message = update.message
        val chat = update.message?.chat
        val user = update.message?.from
        val text = update.message?.text
        if (chat != null && message != null && user != null && text != null) {
            handleCommand(bot, update, message, chat, user, text)
        }
    }

    protected abstract fun handleCommand(bot: Bot, update: Update, message: Message, chat: Chat, user: User, text: String)
}
