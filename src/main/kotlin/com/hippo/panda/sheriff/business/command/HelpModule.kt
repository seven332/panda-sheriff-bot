package com.hippo.panda.sheriff.business.command

import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.Chat
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.Update
import me.ivmg.telegram.entities.User

class HelpModule: CommandModule() {
    override val isAdminOnly: Boolean
        get() = true

    override val command: String
        get() = "help"

    override val description: String
        get() = "/help"

    override fun handleCommand(bot: Bot, update: Update, message: Message, chat: Chat, user: User, text: String) {
        bot.sendMessage(chat.id, "Panda Sheriff 为您服务❤️\n" +
                commandDescriptions.values.joinToString("\n") + "\n" +
                "更多特殊服务⬇️\n" +
                "https://github.com/seven332/panda-sheriff-bot")
    }
}
