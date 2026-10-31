package com.denser.june.core.utils

private val HEX_CHARS = "0123456789abcdef".toCharArray()

/**
 * Encodes this byte array as a lowercase hexadecimal string.
 *
 * Produces the exact same output as `joinToString("") { "%02x".format(it) }` but avoids
 * allocating a [java.util.Formatter] for every single byte. Content hashes are computed
 * for every journal several times per sync, so that allocation cost adds up.
 */
fun ByteArray.toHexString(): String {
    val chars = CharArray(size * 2)
    forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xFF
        chars[index * 2] = HEX_CHARS[value ushr 4]
        chars[index * 2 + 1] = HEX_CHARS[value and 0x0F]
    }
    return String(chars)
}
