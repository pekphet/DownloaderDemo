package com.fish.downloader.framework

import java.util.concurrent.Executors

/**
 * Created by fish on 17-9-6.
 */
class ThreadPool {
    companion object {
        val TAG = "THREAD POOL OF KT."
        val sThreadService by lazy { Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2) }

        fun addTask(task: Runnable) = sThreadService.submit(task)
    }
}