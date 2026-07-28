package com.nanji.lootarchive.util

/**
 * 照片传递队列 — 绕过 Compose State 快照机制，直接在 CameraScreen 和 AddItemScreen 之间传递路径
 */
object PhotoQueue {
    private val lock = Any()
    private val pending = mutableListOf<String>()

    fun enqueue(paths: List<String>) {
        synchronized(lock) { pending.addAll(paths) }
    }

    fun consume(): List<String> {
        synchronized(lock) {
            val result = pending.toList()
            pending.clear()
            return result
        }
    }
}
