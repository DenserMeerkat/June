package com.denser.june.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class JournalTheme(
    val primaryColor: Int,
    val style: String = "TonalSpot",
    val secondaryColor: Int? = null,
    val tertiaryColor: Int? = null,
    val neutralColor: Int? = null
)
