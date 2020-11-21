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

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

interface JobScheduler {
    fun schedule(block: () -> Unit)
    fun schedule(delay: Long, block: () -> Unit): Int
    fun scheduleAt(time: Long, block: () -> Unit): Int
    fun cancel(id: Int)
}

class RealJobScheduler: JobScheduler {
    private val idGenerator = AtomicInteger(1)
    private val futures = ConcurrentHashMap<Int, ScheduledFuture<*>>()
    private val scheduleExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val blockExecutor: Executor = Executors.newCachedThreadPool()

    override fun schedule(block: () -> Unit) {
        blockExecutor.execute(block)
    }

    override fun schedule(delay: Long, block: () -> Unit): Int {
        // Get not 0 id
        var id: Int
        do {
            id = idGenerator.getAndIncrement()
        } while (id == 0)

        // Fix time
        val newTime = if (delay < 0) 0 else delay

        val future = scheduleExecutor.schedule({
            futures.remove(id)
            blockExecutor.execute(block)
        }, newTime, TimeUnit.MILLISECONDS)
        futures[id] = future
        return id
    }

    override fun scheduleAt(time: Long, block: () -> Unit): Int {
        return schedule(time - System.currentTimeMillis(), block)
    }

    override fun cancel(id: Int) {
        futures.remove(id)?.cancel(false)
    }
}
