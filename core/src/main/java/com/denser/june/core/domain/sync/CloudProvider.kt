package com.denser.june.core.domain.sync

import com.denser.june.core.domain.model.Journal
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class RemoteFileMeta(
    val name: String,
    val lastModified: Long
)

interface CloudProvider {
    val name: String

    /**
     * Authenticate or reconnect with the provider.
     */
    suspend fun connect(): Result<Unit>

    /**
     * Check if the user is currently authenticated.
     */
    fun isConnected(): Flow<Boolean>

    /**
     * Disconnect and clear local tokens.
     */
    suspend fun disconnect()

    /**
     * Upload a journal entry (metadata) to the cloud.
     */
    suspend fun uploadJournal(journal: Journal): Result<String>

    /**
     * Download a journal entry from the cloud.
     */
    suspend fun downloadJournal(cloudId: String): Result<Journal>

    /**
     * Upload a media file to the cloud.
     */
    suspend fun uploadMedia(file: File): Result<String>

    /**
     * Download a media file from the cloud.
     */
    suspend fun downloadMedia(cloudId: String, targetFile: File): Result<File>


    /**
     * Update the sync manifest in the cloud.
     */
    suspend fun updateManifest(manifest: SyncManifest): Result<Unit>

    /**
     * List all journal filenames and metadata in the cloud.
     */
    suspend fun listJournals(): Result<List<RemoteFileMeta>>

    /**
     * List all media filenames in the cloud.
     */
    suspend fun listMedia(): Result<List<String>>

    /**
     * Delete a media file from the cloud.
     */
    suspend fun deleteMedia(filename: String): Result<Unit>
}

@Serializable
data class SyncManifest(
    val lastSyncTime: Long,
    val lastSyncDeviceId: String,
    val databaseVersion: Int,
    val schemaVersion: Int,
    val totalJournals: Int,
    val totalMedia: Int = 0
)
