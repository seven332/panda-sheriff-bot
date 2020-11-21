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

package com.hippo.panda.sheriff.business

import com.hippo.panda.sheriff.base.*
import com.hippo.panda.sheriff.util.ban
import com.hippo.panda.sheriff.util.toMarkdown
import com.hippo.panda.sheriff.util.unban
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.*
import kotlin.random.Random

class NewMemberModule: Module {
    override fun willCreateBot(register: Register) {
        register.register(::checkNewChatMember, ::handleNewChatMember)
        register.register(::checkRecaptchaCallback, ::handleRecaptchaCallback)
    }

    override fun didCreateBot(bot: Bot) {
        store.getAllRecaptchas().forEach {
            jobScheduler.schedule(it.life) { banUserForRecaptcha(bot, it.id) }
        }
    }

    private fun checkNewChatMember(update: Update): Boolean {
        return update.message?.newChatMember != null
    }

    private fun handleNewChatMember(bot: Bot, update: Update) {
        val message = update.message
        val chat = update.message?.chat
        val user = update.message?.newChatMember
        if (message == null || chat == null || user == null) return

        bot.ban(chat, user)
        showRecaptcha(bot, message, chat, user)
    }

    private fun showRecaptcha(bot: Bot, message: Message, chat: Chat, user: User) {
        val recaptcha = Recaptcha(
            id = 0,
            chatId = chat.id,
            userId = user.id,
            messageId = 0,
            jobId = 0,
            life = configure.recaptchaLife
        )
        val recaptchaId = store.createRecaptcha(recaptcha)

        // Send recaptcha message
        val animals = adorableAnimals.shuffled().subList(0, 4)
        val answerIndex = Random.nextInt(4)
        val answer = animals[answerIndex]
        val buttons = animals
            .mapIndexed { index, animal ->
                InlineKeyboardButton(
                    "${animal.emoji} ${animal.chinese} ${animal.english}",
                    callbackData = "recaptcha:${if (index == answerIndex) recaptchaId else -index}"
                )
            }
            .map { listOf(it) }
        val text = """
            ${user.toMarkdown()}
            回答问题以解除禁言。
            Answer the questions to unban yourself.
            ➡${answer.emoji}⬅
            ⇨${answer.chinese}⇦
            -> ${answer.english} <-
        """.trimIndent()
        val replyMarkup = InlineKeyboardMarkup(buttons)
        val result = bot.sendMessage(
            chat.id,
            text,
            parseMode = ParseMode.MARKDOWN,
            replyToMessageId = message.messageId,
            replyMarkup = replyMarkup
        )
        val recaptchaMessageId = result.first?.body()?.result?.messageId ?: return

        val jobId = jobScheduler.schedule(configure.recaptchaLife) { banUserForRecaptcha(bot, recaptchaId) }

        store.updateRecaptcha(recaptchaId, recaptcha.duplicate(recaptchaMessageId, jobId))
    }

    private fun banUserForRecaptcha(bot: Bot, recaptchaId: Int) {
        val recaptcha = store.findRecaptcha(recaptchaId) ?: return
        store.deleteRecaptcha(recaptchaId)

        jobScheduler.cancel(recaptcha.jobId)

        bot.deleteMessage(recaptcha.chatId, recaptcha.messageId)

        val user = store.findUser(recaptcha.userId) ?: return
        val text = "大约 ${user.toMarkdown()} 的确说不了话。"
        bot.sendMessage(recaptcha.chatId, text, parseMode = ParseMode.MARKDOWN)
    }

    private fun checkRecaptchaCallback(update: Update): Boolean {
        return update.callbackQuery?.data?.startsWith("recaptcha:") ?: false
    }

    private fun handleRecaptchaCallback(bot: Bot, update: Update) {
        val recaptchaId = update.callbackQuery?.data?.split(":")?.getOrNull(1)?.toIntOrNull() ?: return
        val recaptcha = store.findRecaptcha(recaptchaId) ?: return

        // Verify user
        val user = update.callbackQuery?.from ?: return
        if (recaptcha.userId != user.id) return

        store.deleteRecaptcha(recaptchaId)
        jobScheduler.cancel(recaptcha.jobId)
        bot.deleteMessage(recaptcha.chatId, recaptcha.messageId)
        bot.unban(recaptcha.chatId, recaptcha.userId)

        val text = "你好，${user.toMarkdown()}！${configure.welcomeMessage}"
        bot.sendMessage(recaptcha.chatId, text, parseMode = ParseMode.MARKDOWN)
    }
}

private data class Animal(val emoji: String, val chinese: String, val english: String)

private val adorableAnimals = listOf(
    Animal("\uD83D\uDC3C", "熊猫", "Panda"),
    Animal("\uD83D\uDC3B", "熊", "Bear"),
    Animal("\uD83E\uDD9A", "孔雀", "Peacock"),
    Animal("\uD83E\uDD8E", "蜥蜴", "Lizard"),
    Animal("\uD83E\uDD98", "袋鼠", "Kangaroo"),
    Animal("\uD83E\uDD8A", "狐狸", "Fox"),
    Animal("\uD83E\uDD9D", "浣熊", "Raccoon"),
    Animal("\uD83E\uDD8C", "鹿", "Deer"),
    Animal("\uD83D\uDC18", "大象", "Elephant"),
    Animal("\uD83E\uDD8F", "犀牛", "Rhinoceros"),
    Animal("\uD83D\uDC07", "兔子", "Rabbit"),
    Animal("\uD83D\uDC3F", "松鼠", "Chipmunk"),
    Animal("\uD83E\uDD94", "刺猬", "Hedgehog"),
    Animal("\uD83E\uDD87", "蝙蝠", "Bat"),
    Animal("\uD83E\uDDA1", "獾", "Badger"),
    Animal("\uD83D\uDC27", "企鹅", "Penguin"),
    Animal("\uD83E\uDD89", "猫头鹰", "Owl"),
    Animal("\uD83D\uDC0A", "鳄鱼", "Crocodile"),
    Animal("\uD83D\uDC22", "乌龟", "Turtle"),
    Animal("\uD83D\uDC0D", "蛇", "Snake"),
    Animal("\uD83D\uDC0B", "鲸鱼", "Whale"),
    Animal("\uD83D\uDC2C", "海豚", "Dolphin"),
    Animal("\uD83D\uDC19", "章鱼", "Octopus"),
    Animal("\uD83D\uDC0C", "蜗牛", "Snail"),
    Animal("\uD83E\uDD8B", "蝴蝶", "Butterfly"),
    Animal("\uD83D\uDC67", "正义", "antioppe")
)
