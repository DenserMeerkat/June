package com.denser.june.core.media.exif

object AppleDeviceMap {
    private val APPLE_MODEL_MAP: Map<String, String> = mapOf(
        "iPhone12,1" to "iPhone 11",
        "iPhone12,3" to "iPhone 11 Pro",
        "iPhone12,5" to "iPhone 11 Pro Max",
        "iPhone12,8" to "iPhone SE (2nd generation)",
        "iPhone13,1" to "iPhone 12 mini",
        "iPhone13,2" to "iPhone 12",
        "iPhone13,3" to "iPhone 12 Pro",
        "iPhone13,4" to "iPhone 12 Pro Max",
        "iPhone14,2" to "iPhone 13 Pro",
        "iPhone14,3" to "iPhone 13 Pro Max",
        "iPhone14,4" to "iPhone 13 mini",
        "iPhone14,5" to "iPhone 13",
        "iPhone14,6" to "iPhone SE (3rd generation)",
        "iPhone14,7" to "iPhone 14",
        "iPhone14,8" to "iPhone 14 Plus",
        "iPhone15,2" to "iPhone 14 Pro",
        "iPhone15,3" to "iPhone 14 Pro Max",
        "iPhone15,4" to "iPhone 15",
        "iPhone15,5" to "iPhone 15 Plus",
        "iPhone16,1" to "iPhone 15 Pro",
        "iPhone16,2" to "iPhone 15 Pro Max",
        "iPhone17,1" to "iPhone 16 Pro",
        "iPhone17,2" to "iPhone 16 Pro Max",
        "iPhone17,3" to "iPhone 16",
        "iPhone17,4" to "iPhone 16 Plus",
        "iPhone18,1" to "iPhone 17 Pro",
        "iPhone18,2" to "iPhone 17 Pro Max",
        "iPhone18,3" to "iPhone 17",
        "iPhone18,4" to "iPhone 17 Plus",
        "iPhone19,1" to "iPhone 18 Pro",
        "iPhone19,2" to "iPhone 18 Pro Max"
    )

    fun mapAppleModelIdentifier(raw: String): String {
        val clean = raw.trim()
        return APPLE_MODEL_MAP[clean] ?: clean
    }
}
