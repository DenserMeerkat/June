package com.denser.june.presentation.plus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

val LocalIsPlus = compositionLocalOf { false }
val LocalPlusSheetController = compositionLocalOf<PlusSheetController> {
    error("No PlusSheetController provided")
}

@Stable
class PlusSheetController {
    var isVisible by mutableStateOf(false)
        private set

    private var pendingUnlockCallback: (() -> Unit)? = null

    fun show(onPlusUnlocked: (() -> Unit)? = null) {
        pendingUnlockCallback = onPlusUnlocked
        isVisible = true
    }

    fun dismiss(isUnlocked: Boolean = false) {
        val callback = pendingUnlockCallback
        isVisible = false
        pendingUnlockCallback = null
        if (isUnlocked) {
            callback?.invoke()
        }
    }

    fun onPlusUnlockedSuccess() {
        dismiss(isUnlocked = true)
    }
}

@Composable
fun rememberPlusSheetController(): PlusSheetController {
    return remember { PlusSheetController() }
}
