package com.denser.june.core.domain.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object AppLogger {
    enum class Category {
        SYNC,
        BACKUP,
        DATABASE
    }

    private val enabledCategories = ConcurrentHashMap<Category, Boolean>()
    private val logBuffer = mutableListOf<String>()
    private const val MAX_BUFFER_SIZE = 500

    fun setCategoryEnabled(category: Category, enabled: Boolean) {
        enabledCategories[category] = enabled
    }

    fun isCategoryEnabled(category: Category): Boolean {
        return enabledCategories[category] ?: false
    }

    @Synchronized
    private fun logToBuffer(category: Category, level: String, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val errorMsg = if (throwable != null) "\n${Log.getStackTraceString(throwable)}" else ""
        val entry = "[$timestamp] $level/$tag: $message$errorMsg"
        logBuffer.add(entry)
        if (logBuffer.size > MAX_BUFFER_SIZE) {
            logBuffer.removeAt(0)
        }
    }

    @Synchronized
    fun getBufferedLogs(): List<String> {
        return logBuffer.toList()
    }

    @Synchronized
    fun clearBufferedLogs() {
        logBuffer.clear()
    }

    fun d(category: Category, tag: String, message: String) {
        if (isCategoryEnabled(category)) {
            Log.d(tag, message)
            logToBuffer(category, "D", tag, message)
        }
    }

    fun w(category: Category, tag: String, message: String) {
        if (isCategoryEnabled(category)) {
            Log.w(tag, message)
            logToBuffer(category, "W", tag, message)
        }
    }

    fun e(category: Category, tag: String, message: String, throwable: Throwable? = null) {
        if (isCategoryEnabled(category)) {
            Log.e(tag, message, throwable)
            logToBuffer(category, "E", tag, message, throwable)
        }
    }
}
