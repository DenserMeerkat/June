package com.denser.june.core.domain.sync

import com.denser.june.core.utils.computeSHA256
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
data class CachedFileHash(
    val lastModified: Long,
    val size: Long,
    val hash: String
)

class SyncHashCache(
    private val cacheDirectory: File
) {
    companion object {
        const val CACHE_FILE_NAME = "sha256_cache.json"
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }
    }

    private val cacheFile = File(cacheDirectory, CACHE_FILE_NAME)
    private val memoryCache = ConcurrentHashMap<String, CachedFileHash>()
    private val isDirty = AtomicBoolean(false)
    private val fileLock = Any()

    init {
        loadFromDisk()
    }

    /**
     * Retrieves the cached SHA-256 hash if the file exists and its (lastModified, size) tuple matches.
     * Otherwise computes the SHA-256 hash, stores it in cache, marks the cache dirty, and returns the hash.
     */
    fun getOrCompute(file: File): String {
        if (!file.exists() || file.length() == 0L) return ""
        val path = file.absolutePath
        val currentMtime = file.lastModified()
        val currentSize = file.length()

        val cached = memoryCache[path]
        if (cached != null && cached.lastModified == currentMtime && cached.size == currentSize) {
            return cached.hash
        }

        val newHash = file.computeSHA256()
        memoryCache[path] = CachedFileHash(
            lastModified = currentMtime,
            size = currentSize,
            hash = newHash
        )
        isDirty.set(true)
        return newHash
    }

    /**
     * Explicitly inserts or updates an entry for the specified file.
     */
    fun put(file: File, hash: String) {
        if (!file.exists() || file.length() == 0L || hash.isBlank()) return
        memoryCache[file.absolutePath] = CachedFileHash(
            lastModified = file.lastModified(),
            size = file.length(),
            hash = hash
        )
        isDirty.set(true)
    }

    /**
     * Returns the cached hash if valid for the file, or null if missing or invalidated.
     */
    fun get(file: File): String? {
        if (!file.exists() || file.length() == 0L) return null
        val cached = memoryCache[file.absolutePath] ?: return null
        if (cached.lastModified == file.lastModified() && cached.size == file.length()) {
            return cached.hash
        }
        return null
    }

    /**
     * Persists memory cache to disk if any modifications occurred since last persistence.
     */
    fun persistIfDirty() {
        if (!isDirty.compareAndSet(true, false)) return
        saveToDisk()
    }

    /**
     * Clears both in-memory cache and persisted cache file.
     */
    fun clear() {
        memoryCache.clear()
        isDirty.set(false)
        synchronized(fileLock) {
            try {
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Returns the number of entries currently stored in the cache.
     */
    fun size(): Int = memoryCache.size

    private fun loadFromDisk() {
        synchronized(fileLock) {
            try {
                if (!cacheFile.exists()) return
                val text = cacheFile.readText()
                if (text.isBlank()) return
                val loaded = json.decodeFromString<Map<String, CachedFileHash>>(text)
                memoryCache.putAll(loaded)
            } catch (e: Exception) {
                // If corrupted or unreadable, ignore and start with empty cache
            }
        }
    }

    private fun saveToDisk() {
        synchronized(fileLock) {
            try {
                if (!cacheDirectory.exists()) {
                    cacheDirectory.mkdirs()
                }
                val snapshot = HashMap(memoryCache)
                val jsonString = json.encodeToString(snapshot)
                val tempFile = File(cacheDirectory, "$CACHE_FILE_NAME.tmp")
                tempFile.writeText(jsonString)
                if (tempFile.exists()) {
                    try {
                        Files.move(
                            tempFile.toPath(),
                            cacheFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                        )
                    } catch (e: AtomicMoveNotSupportedException) {
                        Files.move(
                            tempFile.toPath(),
                            cacheFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                }
            } catch (e: Exception) {
                // Disk persistence failure should not disrupt memory cache operations
            }
        }
    }
}
