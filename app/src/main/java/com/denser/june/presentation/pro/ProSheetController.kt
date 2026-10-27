package com.denser.june.presentation.pro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

val LocalIsPro = compositionLocalOf { false }
val LocalProSheetController = compositionLocalOf<ProSheetController> {
    error("No ProSheetController provided")
}

@Stable
class ProSheetController {
    var isVisible by mutableStateOf(false)
        private set

    private var pendingUnlockCallback: (() -> Unit)? = null

    fun show(onProUnlocked: (() -> Unit)? = null) {
        pendingUnlockCallback = onProUnlocked
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

    fun onProUnlockedSuccess() {
        dismiss(isUnlocked = true)
    }
}

@Composable
fun rememberProSheetController(): ProSheetController {
    return remember { ProSheetController() }
}
