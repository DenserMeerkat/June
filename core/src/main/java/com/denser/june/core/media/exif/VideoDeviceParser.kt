package com.denser.june.core.media.exif

import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale

object VideoDeviceParser {
    private const val FOURCC_DATA = 0x64617461
    private val REGEX_NUMBERS_ONLY = Regex("^[0-9.]+$")
    private val REGEX_VALID_MAKE = Regex("^[a-zA-Z][a-zA-Z0-9 .'-]{1,30}$")
    private val REGEX_LENS_CANDIDATE = Regex("(?i)\\b(back|front|camera|triple|dual|single|lens)\\b")

    fun extractVideoDeviceInfo(pfd: ParcelFileDescriptor): String? {
        return try {
            FileInputStream(pfd.fileDescriptor).use { stream ->
                extractVideoDeviceInfo(stream.channel)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractVideoDeviceInfo(file: File): String? {
        if (!file.exists() || file.length() < 32L) return null
        return try {
            FileInputStream(file).use { stream ->
                extractVideoDeviceInfo(stream.channel)
            }
        } catch (_: Exception) {
            try {
                file.inputStream().use { extractVideoDeviceInfo(it) }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun extractVideoDeviceInfo(inputStream: InputStream): String? {
        return try {
            val buffer = ByteArray(512 * 1024)
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 32) return null
            parseDeviceInfoFromBuffer(buffer, bytesRead)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractVideoDeviceInfo(channel: FileChannel): String? {
        val length = channel.size()
        if (length < 32L) return null

        var moovBytes: ByteArray? = null
        var pos = 0L
        val headerBuf = ByteBuffer.allocate(16)

        while (pos < length - 8L) {
            channel.position(pos)
            headerBuf.clear()
            val read = channel.read(headerBuf)
            if (read < 8) break
            headerBuf.flip()
            val sizeInt = headerBuf.int.toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4)
            headerBuf.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)
            val atomSize = when (sizeInt) {
                1L -> {
                    if (read < 16) channel.read(headerBuf)
                    headerBuf.long
                }
                0L -> length - pos
                else -> sizeInt
            }
            if (atomSize <= 8L && sizeInt != 0L) break
            if (type == "moov") {
                val moovLen = minOf(atomSize, 4 * 1024 * 1024L).toInt()
                val buffer = ByteArray(moovLen)
                channel.position(pos)
                val bb = ByteBuffer.wrap(buffer)
                while (bb.hasRemaining()) {
                    if (channel.read(bb) <= 0) break
                }
                moovBytes = buffer
                break
            }
            pos += atomSize
        }

        if (moovBytes != null) {
            parseDeviceInfoFromBuffer(moovBytes, moovBytes.size)?.let { return it }
        }

        val tailLen = minOf(length, 2 * 1024 * 1024L).toInt()
        val tailBuffer = ByteArray(tailLen)
        channel.position(length - tailLen)
        val bbTail = ByteBuffer.wrap(tailBuffer)
        var tailRead = 0
        while (bbTail.hasRemaining()) {
            val r = channel.read(bbTail)
            if (r <= 0) break
            tailRead += r
        }
        parseDeviceInfoFromBuffer(tailBuffer, tailRead)?.let { return it }

        val headLen = minOf(length, 512 * 1024L).toInt()
        val headBuffer = ByteArray(headLen)
        channel.position(0L)
        val bbHead = ByteBuffer.wrap(headBuffer)
        var headRead = 0
        while (bbHead.hasRemaining()) {
            val r = channel.read(bbHead)
            if (r <= 0) break
            headRead += r
        }
        return parseDeviceInfoFromBuffer(headBuffer, headRead)
    }

    private fun parseDeviceInfoFromBuffer(buffer: ByteArray, bytesRead: Int): String? {
        val (qtMake, qtModel) = parseQuickTimeMetadata(buffer, bytesRead)
        resolveDeviceName(qtMake, qtModel)?.let { return it }

        searchAppleDevicePattern(buffer, bytesRead, "iPhone")?.let { return it }
        searchAppleDevicePattern(buffer, bytesRead, "iPad")?.let { return it }

        val xmpModel = extractXmlTagContent(buffer, bytesRead, "tiff:Model")
        if (isValidMetadataString(xmpModel)) {
            val xmpMake = extractXmlTagContent(buffer, bytesRead, "tiff:Make")
            resolveDeviceName(xmpMake, xmpModel)?.let { return it }
        }

        return null
    }

    private fun searchAppleDevicePattern(buffer: ByteArray, bytesRead: Int, prefix: String): String? {
        val pattern = prefix.toByteArray(Charsets.UTF_8)
        var searchPos = 0
        while (searchPos < bytesRead - 8) {
            val idx = indexOfBytes(buffer, bytesRead, pattern, searchPos)
            if (idx == -1) break
            var end = idx + pattern.size
            val maxSearch = minOf(bytesRead, idx + 30)
            while (end < maxSearch) {
                val b = buffer[end]
                if (b in 32..126 && b != '/'.code.toByte() && b != '<'.code.toByte() && b != '>'.code.toByte()) {
                    end++
                } else {
                    break
                }
            }
            val raw = String(buffer, idx, end - idx, Charsets.UTF_8).trim()
            val clean = raw.takeWhile { it.isLetterOrDigit() || it == ' ' || it == ',' }.trim()
            if (clean.startsWith("${prefix} OS", ignoreCase = true) || clean.startsWith("${prefix}OS", ignoreCase = true)) {
                searchPos = idx + pattern.size
                continue
            }
            val minLen = if (prefix == "iPhone") 6 else 5
            val valid = if (prefix == "iPhone") clean.any { it.isDigit() } else true
            if (clean.length in minLen..28 && valid && isValidMetadataString(clean)) {
                val mapped = AppleDeviceMap.mapAppleModelIdentifier(clean)
                return if (mapped.startsWith("Apple", ignoreCase = true)) mapped else "Apple $mapped"
            }
            searchPos = idx + pattern.size
        }
        return null
    }

    private fun extractXmlTagContent(buffer: ByteArray, bytesRead: Int, tagName: String): String? {
        val startTag = "<$tagName>".toByteArray(Charsets.UTF_8)
        val endTag = "</$tagName>".toByteArray(Charsets.UTF_8)
        val startIdx = indexOfBytes(buffer, bytesRead, startTag)
        if (startIdx == -1) return null
        val contentStart = startIdx + startTag.size
        val maxEnd = minOf(bytesRead, contentStart + 64)
        val endIdx = indexOfBytes(buffer, maxEnd, endTag, contentStart)
        if (endIdx > contentStart) {
            return String(buffer, contentStart, endIdx - contentStart, Charsets.UTF_8).trim()
        }
        return null
    }

    private fun parseQuickTimeMetadata(buffer: ByteArray, length: Int): Pair<String?, String?> {
        var make: String? = null
        var model: String? = null
        val keyMap = mutableMapOf<Int, String>()

        val keysIdx = indexOfBytes(buffer, length, "keys".toByteArray(Charsets.US_ASCII))
        val ilstIdx = indexOfBytes(buffer, length, "ilst".toByteArray(Charsets.US_ASCII))

        if (keysIdx >= 4 && ilstIdx >= 4) {
            try {
                val keysStart = keysIdx - 4
                val bbKeys = ByteBuffer.wrap(buffer, keysStart, length - keysStart)
                val keysSize = bbKeys.int
                if (keysSize > 12 && bbKeys.remaining() >= 8) {
                    bbKeys.int
                    bbKeys.int
                    val entryCount = bbKeys.int
                    for (i in 1..entryCount) {
                        if (bbKeys.remaining() < 8) break
                        val keySize = bbKeys.int
                        if (keySize < 8 || bbKeys.remaining() < keySize - 4) break
                        bbKeys.int
                        val strLen = keySize - 8
                        if (strLen in 1..128 && bbKeys.remaining() >= strLen) {
                            val keyBytes = ByteArray(strLen)
                            bbKeys.get(keyBytes)
                            val kName = String(keyBytes, Charsets.UTF_8).trim()
                            keyMap[i] = kName
                        } else {
                            break
                        }
                    }
                }
            } catch (_: Exception) {}

            if (keyMap.isNotEmpty()) {
                var lensModel: String? = null
                try {
                    val ilstStart = ilstIdx - 4
                    val bbIlst = ByteBuffer.wrap(buffer, ilstStart, length - ilstStart)
                    val ilstSize = bbIlst.int
                    if (ilstSize > 8 && bbIlst.remaining() >= 4) {
                        bbIlst.int
                        val ilstEnd = if (ilstStart + ilstSize < length) ilstStart + ilstSize else length
                        while (bbIlst.position() < ilstEnd - 8) {
                            val itemPos = bbIlst.position()
                            val itemSize = bbIlst.int
                            if (itemSize < 8 || itemPos + itemSize > ilstEnd) break
                            val keyIndex = bbIlst.int
                            val keyName = keyMap[keyIndex]

                            while (bbIlst.position() < itemPos + itemSize - 8) {
                                val subSize = bbIlst.int
                                val subType = bbIlst.int
                                if (subType == FOURCC_DATA) {
                                    if (subSize >= 16 && bbIlst.position() + subSize - 8 <= itemPos + itemSize) {
                                        bbIlst.int
                                        bbIlst.int
                                        val valLen = subSize - 16
                                        if (valLen in 1..128) {
                                            val valBytes = ByteArray(valLen)
                                            bbIlst.get(valBytes)
                                            val strVal = String(valBytes, Charsets.UTF_8).trim()
                                            if (keyName != null && isValidMetadataString(strVal)) {
                                                val isModel = keyName.equals("com.apple.quicktime.model", ignoreCase = true) ||
                                                    keyName.equals("com.android.model", ignoreCase = true) ||
                                                    keyName.equals("model", ignoreCase = true) ||
                                                    (keyName.endsWith(".model", ignoreCase = true) && !keyName.contains("lens", ignoreCase = true))

                                                val isMake = keyName.equals("com.apple.quicktime.make", ignoreCase = true) ||
                                                    keyName.equals("com.android.manufacturer", ignoreCase = true) ||
                                                    keyName.equals("make", ignoreCase = true) ||
                                                    keyName.equals("manufacturer", ignoreCase = true) ||
                                                    ((keyName.endsWith(".make", ignoreCase = true) || keyName.endsWith(".manufacturer", ignoreCase = true)) && !keyName.contains("maker", ignoreCase = true))

                                                val isLensModel = keyName.contains("lens_model", ignoreCase = true) ||
                                                    keyName.contains("lensmodel", ignoreCase = true)

                                                if (isModel) {
                                                    model = strVal
                                                } else if (isMake && isValidMakeString(strVal)) {
                                                    make = strVal
                                                } else if (isLensModel) {
                                                    lensModel = strVal
                                                }
                                            }
                                        }
                                    }
                                    break
                                } else {
                                    if (subSize <= 8) break
                                    bbIlst.position(bbIlst.position() + subSize - 8)
                                }
                            }
                            bbIlst.position(itemPos + itemSize)
                        }
                    }
                } catch (_: Exception) {}

                if (model == null && lensModel != null) {
                    val candidate = lensModel.split(REGEX_LENS_CANDIDATE).firstOrNull()?.trim()
                    if (!candidate.isNullOrBlank() && isValidMetadataString(candidate) && candidate.length >= 3) {
                        model = candidate
                    }
                }
                if (make == null && model != null) {
                    if (model.startsWith("iPhone", ignoreCase = true) || model.startsWith("iPad", ignoreCase = true)) {
                        make = "Apple"
                    } else if (model.startsWith("Pixel", ignoreCase = true)) {
                        make = "Google"
                    }
                }
            }
        }

        if (keyMap.isEmpty()) {
            if (model == null) {
                val qtModelPattern = "com.apple.quicktime.model".toByteArray(Charsets.US_ASCII)
                val modelKeyIdx = indexOfBytes(buffer, length, qtModelPattern)
                if (modelKeyIdx != -1) {
                    val dataPattern = "data".toByteArray(Charsets.US_ASCII)
                    val searchLimit = minOf(length, modelKeyIdx + 512)
                    var dIdx = indexOfBytes(buffer, searchLimit, dataPattern, modelKeyIdx + qtModelPattern.size)
                    while (dIdx != -1 && dIdx + 12 < length) {
                        val valStart = dIdx + 12
                        var valEnd = valStart
                        val maxVal = minOf(length, valStart + 64)
                        while (valEnd < maxVal && buffer[valEnd] in 32..126) {
                            valEnd++
                        }
                        val candidate = String(buffer, valStart, valEnd - valStart, Charsets.UTF_8).trim()
                        if (isValidMetadataString(candidate) && candidate.length in 3..40) {
                            model = candidate
                            break
                        }
                        dIdx = indexOfBytes(buffer, searchLimit, dataPattern, dIdx + 4)
                    }
                }
            }

            if (make == null && (model == null || (!model.startsWith("iPhone", ignoreCase = true) && !model.startsWith("iPad", ignoreCase = true)))) {
                val qtMakePattern = "com.apple.quicktime.make".toByteArray(Charsets.US_ASCII)
                val makeKeyIdx = indexOfBytes(buffer, length, qtMakePattern)
                if (makeKeyIdx != -1) {
                    val dataPattern = "data".toByteArray(Charsets.US_ASCII)
                    val searchLimit = minOf(length, makeKeyIdx + 512)
                    var dIdx = indexOfBytes(buffer, searchLimit, dataPattern, makeKeyIdx + qtMakePattern.size)
                    while (dIdx != -1 && dIdx + 12 < length) {
                        val valStart = dIdx + 12
                        var valEnd = valStart
                        val maxVal = minOf(length, valStart + 64)
                        while (valEnd < maxVal && buffer[valEnd] in 32..126) {
                            valEnd++
                        }
                        val candidate = String(buffer, valStart, valEnd - valStart, Charsets.UTF_8).trim()
                        if (isValidMakeString(candidate) && candidate.length in 2..40) {
                            make = candidate
                            break
                        }
                        dIdx = indexOfBytes(buffer, searchLimit, dataPattern, dIdx + 4)
                    }
                }
            }
        }

        if (model != null) {
            if (model.startsWith("iPhone", ignoreCase = true) || model.startsWith("iPad", ignoreCase = true)) {
                make = "Apple"
            }
        }

        return Pair(make, model)
    }

    private fun indexOfBytes(source: ByteArray, size: Int, target: ByteArray, startIndex: Int = 0): Int {
        if (target.isEmpty() || size < target.size || startIndex < 0) return -1
        val limit = minOf(size, source.size) - target.size
        for (i in startIndex..limit) {
            var match = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    fun isValidMetadataString(s: String?): Boolean {
        if (s.isNullOrBlank()) return false
        if (s.contains('\uFFFD')) return false
        if (s.any { it.code < 32 && it != '\t' && it != '\n' }) return false
        if (s.matches(REGEX_NUMBERS_ONLY)) return false
        return true
    }

    fun isValidMakeString(s: String?): Boolean {
        if (s.isNullOrBlank()) return false
        if (s.contains('\uFFFD') || s.contains('+') || s.contains('/') || s.contains('\\') || s.contains(':') || s.contains('=')) return false
        if (s.any { it.code < 32 && it != '\t' && it != '\n' }) return false
        val trimmed = s.trim()
        if (!trimmed.matches(REGEX_VALID_MAKE)) return false
        return true
    }

    fun resolveDeviceName(make: String?, rawModel: String?): String? {
        if (!isValidMetadataString(rawModel)) return null
        val trimmedModel = rawModel!!.trim()
        if (trimmedModel.length < 3) return null

        val model = AppleDeviceMap.mapAppleModelIdentifier(trimmedModel)
        if (model.startsWith("iPhone", ignoreCase = true) || model.startsWith("iPad", ignoreCase = true)) {
            return if (model.startsWith("Apple", ignoreCase = true)) model else "Apple $model"
        }

        val cleanMake = make?.trim()?.takeIf { isValidMakeString(it) }
        val mfg = cleanMake?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        return when {
            mfg.isNullOrBlank() -> model
            model.startsWith(mfg, ignoreCase = true) -> model
            else -> "$mfg $model"
        }
    }
}
