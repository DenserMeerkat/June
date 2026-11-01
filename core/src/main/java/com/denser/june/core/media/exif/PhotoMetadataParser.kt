package com.denser.june.core.media.exif

import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.round

data class ParsedPhotoInfo(
    val dateTakenMillis: Long,
    val deviceName: String,
    val aperture: String?,
    val shutterSpeed: String?,
    val focalLength: String?,
    val iso: String?,
    val width: Int,
    val height: Int,
    val megapixels: String?,
    val resolution: String?,
    val isJuneOptimized: Boolean,
    val isHdr: Boolean
)

object PhotoMetadataParser {

    fun parsePhotoExif(
        exif: ExifInterface?,
        rawWidth: Int,
        rawHeight: Int,
        isScreenshot: Boolean,
        fallbackDateTime: Long?
    ): ParsedPhotoInfo {
        val parsedDate = try {
            exif?.dateTimeOriginal?.takeIf { it > 0L }
                ?: exif?.dateTime?.takeIf { it > 0L }
                ?: exif?.dateTimeDigitized?.takeIf { it > 0L }
        } catch (_: Exception) { null }

        val dateTakenMillis = parsedDate ?: run {
            val dateStr = try {
                exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: exif?.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
            } catch (_: Exception) { null }
            parseExifDate(dateStr)
        } ?: fallbackDateTime ?: System.currentTimeMillis()

        val make = try { exif?.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty() } catch (_: Exception) { "" }
        val model = try { exif?.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty() } catch (_: Exception) { "" }
        val software = try { exif?.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim() } catch (_: Exception) { null }
        val device = VideoDeviceParser.resolveDeviceName(make, model) ?: if (software?.startsWith("Android", ignoreCase = true) == true || isScreenshot) "Screenshot" else "Photo Details"

        val apertureStr = parseAperture(exif)
        val shutterStr = parseShutterSpeed(exif?.getAttribute(ExifInterface.TAG_EXPOSURE_TIME))
        val focalStr = parseFocalLength(exif)
        val isoStr = parseIso(exif)

        val orientation = try {
            exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }

        var w = if (rawWidth > 0) rawWidth else try { exif?.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) ?: 0 } catch (_: Exception) { 0 }
        var h = if (rawHeight > 0) rawHeight else try { exif?.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) ?: 0 } catch (_: Exception) { 0 }

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
        ) {
            val temp = w
            w = h
            h = temp
        }

        val mpStr = if (w > 0 && h > 0) String.format(Locale.getDefault(), "%.1f MP", (w.toLong() * h) / 1_000_000f) else null
        val resStr = if (w > 0 && h > 0) "$w × $h" else null

        val isJuneOpt = try { exif?.getAttribute(ExifInterface.TAG_USER_COMMENT) == "june:optimized" } catch (_: Exception) { false }
        val isHdr = isHdrPhoto(exif)

        return ParsedPhotoInfo(
            dateTakenMillis = dateTakenMillis,
            deviceName = device,
            aperture = apertureStr,
            shutterSpeed = shutterStr,
            focalLength = focalStr,
            iso = isoStr,
            width = w,
            height = h,
            megapixels = mpStr,
            resolution = resStr,
            isJuneOptimized = isJuneOpt,
            isHdr = isHdr
        )
    }

    private fun isHdrPhoto(exif: ExifInterface?): Boolean {
        if (exif == null) return false
        return try {
            val transfer = exif.getAttributeInt(ExifInterface.TAG_TRANSFER_FUNCTION, 0)
            if (transfer in listOf(6, 7)) return true
            val xmp = exif.getAttribute(ExifInterface.TAG_XMP)
            if (xmp != null && (xmp.contains("hdrgm:Version") || xmp.contains("HDRGainMap") || xmp.contains("GainMap"))) {
                return true
            }
            exif.getAttribute("GainMap") != null
        } catch (_: Exception) {
            false
        }
    }

    private fun parseAperture(exif: ExifInterface?): String? {
        if (exif == null) return null
        val fNum = parseExifDouble(exif, ExifInterface.TAG_F_NUMBER)
            ?: parseExifDouble(exif, ExifInterface.TAG_APERTURE_VALUE)
        return fNum?.let { String.format(Locale.getDefault(), "f/%.1f", it).removeSuffix(".0") }
    }

    private fun parseShutterSpeed(rawExp: String?): String? {
        if (rawExp.isNullOrBlank()) return null
        return try {
            if (rawExp.contains('/')) {
                val parts = rawExp.split('/')
                val num = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                val den = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
                if (num != null && den != null && den > 0.0) {
                    val sec = num / den
                    if (sec < 1.0) "1/${round(den / num).toInt()}s" else "${String.format(Locale.getDefault(), "%.1f", sec)}s"
                } else "${rawExp}s"
            } else {
                val sec = rawExp.toDoubleOrNull()
                if (sec != null && sec > 0.0) {
                    if (sec < 1.0) "1/${round(1.0 / sec).toInt()}s" else "${String.format(Locale.getDefault(), "%.1f", sec)}s"
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseFocalLength(exif: ExifInterface?): String? {
        if (exif == null) return null
        val focal35 = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0).takeIf { it > 0 }
        val focalPhys = parseExifDouble(exif, ExifInterface.TAG_FOCAL_LENGTH)
        return focal35?.let { "$it mm" } ?: focalPhys?.let { "${round(it).toInt()} mm" }
    }

    private fun parseIso(exif: ExifInterface?): String? {
        if (exif == null) return null
        @Suppress("DEPRECATION")
        val rawIso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
            ?: exif.getAttribute("PhotographicSensitivity")
        return rawIso?.trim()?.takeIf { it.isNotBlank() && it != "0" }?.let { "ISO$it" }
    }

    private fun parseExifDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun parseExifDouble(exif: ExifInterface, tag: String): Double? {
        val d = exif.getAttributeDouble(tag, -1.0)
        if (d > 0.0) return d
        val str = exif.getAttribute(tag) ?: return null
        return if (str.contains('/')) {
            val parts = str.split('/')
            val num = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
            val den = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
            if (num != null && den != null && den != 0.0) num / den else null
        } else {
            str.trim().toDoubleOrNull()
        }
    }
}
