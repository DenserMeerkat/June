package com.denser.june.core.media.exif.model

data class MediaExifInfo(
    val dateTaken: String?,
    val dateTakenMillis: Long?,
    val journalTitle: String? = null,
    val journalMediaCount: Int = 0,
    val deviceName: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val focalLength: String? = null,
    val iso: String? = null,
    val duration: String? = null,
    val fps: String? = null,
    val bitrate: String? = null,
    val qualityLabel: String? = null,
    val fileName: String,
    val width: Int = 0,
    val height: Int = 0,
    val megapixels: String? = null,
    val resolution: String? = null,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val filePath: String,
    val isBackedUp: Boolean = false,
    val isJuneOptimized: Boolean = false,
    val isVideo: Boolean = false,
    val isHdr: Boolean = false,
    val hdrFormat: String? = null
) {
    val isOptimized: Boolean
        get() = isJuneOptimized || (if (isVideo) {
            val shortEdge = minOf(width, height).takeIf { it > 0 } ?: height
            !isHdr && (
                shortEdge <= 480 ||
                fileSizeBytes <= 5 * 1024 * 1024L ||
                (shortEdge <= 720 && fileSizeBytes <= 25 * 1024 * 1024L) ||
                (shortEdge <= 1080 && fileSizeBytes <= 50 * 1024 * 1024L) ||
                (shortEdge <= 1440 && fileSizeBytes <= 100 * 1024 * 1024L)
            )
        } else {
            fileSizeBytes <= 300 * 1024L ||
            (maxOf(width, height) <= 1080 && fileSizeBytes <= 800 * 1024L) ||
            (maxOf(width, height) <= 2048 && fileSizeBytes <= 1.2 * 1024 * 1024L) ||
            (width == 0 && height == 0 && fileSizeBytes <= 500 * 1024L)
        })
}
