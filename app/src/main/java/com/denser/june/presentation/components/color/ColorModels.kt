package com.denser.june.presentation.components.color

enum class ColorScreenTab {
    COLOR,
    PALETTE
}

sealed interface ColorSource {
    object App : ColorSource
    data class Media(val sourceLabel: String, val mediaUri: Any?, val seedColor: Int) : ColorSource
    object Custom : ColorSource
}
