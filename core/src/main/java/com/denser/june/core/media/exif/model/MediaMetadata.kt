package com.denser.june.core.media.exif.model

sealed interface MediaMetadata {
    val fileName: String
    val filePath: String
    val fileSizeBytes: Long
    val fileSizeFormatted: String
    val dateTaken: String?
    val dateTakenMillis: Long?
    val deviceName: String?
    val isBackedUp: Boolean
    val journalTitle: String?
    val journalMediaCount: Int
}

data class PhotoMetadata(
    override val fileName: String,
    override val filePath: String,
    override val fileSizeBytes: Long,
    override val fileSizeFormatted: String,
    override val dateTaken: String?,
    override val dateTakenMillis: Long?,
    override val deviceName: String? = null,
    override val isBackedUp: Boolean = false,
    override val journalTitle: String? = null,
    override val journalMediaCount: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val megapixels: String? = null,
    val resolution: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val focalLength: String? = null,
    val iso: String? = null,
    val isHdr: Boolean = false,
    val hdrFormat: String? = null
) : MediaMetadata

data class VideoMetadata(
    override val fileName: String,
    override val filePath: String,
    override val fileSizeBytes: Long,
    override val fileSizeFormatted: String,
    override val dateTaken: String?,
    override val dateTakenMillis: Long?,
    override val deviceName: String? = null,
    override val isBackedUp: Boolean = false,
    override val journalTitle: String? = null,
    override val journalMediaCount: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val resolution: String? = null,
    val duration: String? = null,
    val durationMs: Long = 0L,
    val fps: String? = null,
    val bitrate: String? = null,
    val qualityLabel: String? = null,
    val isHdr: Boolean = false,
    val hdrFormat: String? = null
) : MediaMetadata

data class AudioMetadata(
    override val fileName: String,
    override val filePath: String,
    override val fileSizeBytes: Long,
    override val fileSizeFormatted: String,
    override val dateTaken: String?,
    override val dateTakenMillis: Long?,
    override val deviceName: String? = null,
    override val isBackedUp: Boolean = false,
    override val journalTitle: String? = null,
    override val journalMediaCount: Int = 0,
    val title: String? = null,
    val duration: String? = null,
    val durationMs: Long = 0L,
    val bitrate: String? = null,
    val sampleRate: String? = null,
    val channels: String? = null,
    val audioFormat: String? = null
) : MediaMetadata

