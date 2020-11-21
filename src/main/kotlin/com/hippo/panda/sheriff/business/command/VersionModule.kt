package com.hippo.panda.sheriff.business.command

import com.hippo.panda.sheriff.base.configure
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.Chat
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.Update
import me.ivmg.telegram.entities.User

class VersionModule: CommandModule() {
    override val isAdminOnly: Boolean
        get() = true

    override val command: String
        get() = "version"

    override fun handleCommand(bot: Bot, update: Update, message: Message, chat: Chat, user: User, text: String) {
        bot.sendMessage(chat.id, configure.botVersion)
    }
}
