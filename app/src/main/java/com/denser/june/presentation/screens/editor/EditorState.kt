package com.denser.june.presentation.screens.editor

import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.model.SongFetchProgress
import com.denser.june.core.domain.model.enums.EditorLayoutDirection
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.utils.getTodayAtMidnight
import java.time.DayOfWeek

data class EditorState(
    val journalId: String? = null,
    val title: String = "",
    val content: String = "",
    val emoji: String? = null,
    val images: List<String> = emptyList(),
    val location: JournalLocation? = null,
    val songDetails: SongDetails? = null,
    val audios: List<com.denser.june.core.domain.model.JournalAudio> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val dateTime: Long = getTodayAtMidnight(),
    val isBookmarked: Boolean = false,
    val isLocked: Boolean = false,
    val isArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val isDraft: Boolean = true,
    val isFetchingSong: Boolean = false,
    val songFetchProgress: SongFetchProgress? = null,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val cloudId: String? = null,
    val startOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    val timeFormat: TimeFormat = TimeFormat.TWELVE_HOUR,
    val isMarkdownEnabled: Boolean = true,
    val isKeyboardAutocorrectEnabled: Boolean = true,
    val isKeyboardCapitalizationEnabled: Boolean = true,
    val editorLayoutDirection: EditorLayoutDirection = EditorLayoutDirection.AUTO,
    val librarySongs: List<SongDetails> = emptyList(),
    val unimportedJournalSongs: List<SongDetails> = emptyList(),
    val importingSongKeys: Set<String> = emptySet(),
    val clipTrimmerSong: SongDetails? = null,
    val pendingStagedSong: SongDetails? = null,
    val journalEditSongPending: Boolean = false,
    val processingMediaPaths: Set<String> = emptySet(),
    val processingAudioIds: Set<String> = emptySet(),
    val font: String? = null,
    val fontVariation: FontVariationConfig? = null,
    val initialFont: String? = null,
    val initialFontVariation: FontVariationConfig? = null,
    val theme: JournalTheme? = null,
    val initialTheme: JournalTheme? = null
) {
    val isDeleted: Boolean get() = deletedAt != null
    val isFontDirty: Boolean
        get() = font != initialFont || fontVariation != initialFontVariation
    val isThemeDirty: Boolean
        get() = theme != initialTheme
    val hasContent: Boolean
        get() = title.isNotBlank() ||
                content.isNotBlank() ||
                emoji != null ||
                images.isNotEmpty() ||
                songDetails != null ||
                audios.isNotEmpty() ||
                location != null ||
                tags.isNotEmpty()
}