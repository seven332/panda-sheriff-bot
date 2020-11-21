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

package com.hippo.panda.sheriff

import com.hippo.panda.sheriff.base.RealRegister
import com.hippo.panda.sheriff.base.configure
import com.hippo.panda.sheriff.base.setupDependencies
import com.hippo.panda.sheriff.business.CollectAdminModule
import com.hippo.panda.sheriff.business.CollectUserModule
import com.hippo.panda.sheriff.business.NewMemberModule
import com.hippo.panda.sheriff.business.command.BanModule
import com.hippo.panda.sheriff.business.command.UnbanModule
import me.ivmg.telegram.Bot

fun main() {
    setupDependencies()

    val modules = arrayOf(
        CollectUserModule(),
        CollectAdminModule(),
        NewMemberModule(),
        BanModule(),
        UnbanModule()
    )

    val builder = Bot.Builder()
    builder.token = configure.botToken
    val register = RealRegister(builder.updater.dispatcher, configure.chatId)
    modules.forEach {
        it.willCreateBot(register)
    }
    val bot = builder.build()
    modules.forEach {
        it.didCreateBot(bot)
    }

    bot.startPolling()
}
