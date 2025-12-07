package com.example.june.viewmodels

import com.example.june.core.presentation.screens.settings.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow

class StateLayer {
    val settingsState = MutableStateFlow(SettingsState())
}