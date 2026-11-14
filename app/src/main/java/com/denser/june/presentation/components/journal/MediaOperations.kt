package com.denser.june.presentation.components.journal

data class MediaOperations(
    val onItemSheetToggle: (Boolean) -> Unit = {},
    val onRemoveMedia: (String) -> Unit = {},
    val onMoveToFront: (String) -> Unit = {},
    val onMediaClick: ((String) -> Unit)? = {},
    val onMediaLongClick: ((String) -> Unit)? = null,
    val frontMediaPath: String? = null,
    val onRemoveSong: () -> Unit = {},
    val onSongSheetToggle: (Boolean) -> Unit = {},
    val onTrimSong: () -> Unit = {},
    val onEditJournalSong: () -> Unit = {},
    val onRemoveLocation: () -> Unit = {},
    val onLocationDialogToggle: (Boolean) -> Unit = {},
    val onAudioClick: (String?) -> Unit = {},
    val onRemoveAudio: ((String) -> Unit)? = null
)
