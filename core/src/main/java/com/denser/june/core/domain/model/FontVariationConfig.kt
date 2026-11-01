package com.denser.june.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FontVariationConfig(
    val weight: Float? = null,
    val width: Float? = null,
    val opticalSize: Float? = null,
    val italic: Float? = null,
    val grade: Float? = null,
    val roundness: Float? = null,
    val slant: Float? = null,
    val monospace: Float? = null,
    val casual: Float? = null,
    val cursive: Float? = null,
    val counterWidth: Float? = null,
    val thickStroke: Float? = null,
    val thinStroke: Float? = null,
    val uppercaseHeight: Float? = null,
    val lowercaseHeight: Float? = null,
    val ascenderHeight: Float? = null,
    val descenderDepth: Float? = null,
    val figureHeight: Float? = null,
    val bounce: Float? = null,
    val informality: Float? = null,
    val spacing: Float? = null
) {
    companion object {
        val DEFAULT = FontVariationConfig(width = 100f, roundness = 100f, grade = 0f)
    }
}
