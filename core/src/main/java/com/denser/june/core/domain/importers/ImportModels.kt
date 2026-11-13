package com.denser.june.core.domain.importers

enum class ImportSource(val displayName: String) {
    DailyYou("Daily You"),
    Diarium("Diarium"),
    DayOne("Day One"),
    StoryPad("StoryPad")
}

data class ImportSummary(
    val source: ImportSource,
    val totalJournals: Int,
    val importedJournals: Int,
    val updatedJournals: Int = 0,
    val mediaExtracted: Int = 0,
    val audiosExtracted: Int = 0,
    val skippedJournals: Int = 0,
    val versionWarning: String? = null,
    val errors: List<String> = emptyList()
)
