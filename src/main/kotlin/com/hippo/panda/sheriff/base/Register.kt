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

import me.ivmg.telegram.Bot
import me.ivmg.telegram.dispatcher.Dispatcher
import me.ivmg.telegram.dispatcher.handlers.Handler
import me.ivmg.telegram.entities.Update

interface Register {
    fun register(checker: (update: Update) -> Boolean, handler: (bot: Bot, update: Update) -> Unit)
}

class RealRegister(private val dispatcher: Dispatcher, private val chatId: Long): Register {
    override fun register(checker: (update: Update) -> Boolean, handler: (bot: Bot, update: Update) -> Unit) {
        dispatcher.addHandler(object : Handler(handler) {
            override val groupIdentifier: String
                get() = "SpecifiedChat"

            override fun checkUpdate(update: Update): Boolean {
                return matches(update, chatId) && checker(update)
            }

            private fun matches(update: Update, chatId: Long): Boolean {
                return update.message?.chat?.id == chatId || update.callbackQuery?.message?.chat?.id == chatId
            }
        })
    }
}
