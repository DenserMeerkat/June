package com.denser.june.presentation.utils

import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.TagCategory

object TagUtils {

    const val TAG_CHARACTER_LIMIT = 25

    data class CategoryUiSpec(
        val iconRes: Int,
        val filledIconRes: Int,
        val color: Color,
        val containerColor: Color,
        @StringRes val emptyMessageRes: Int,
        @StringRes val descriptionRes: Int
    )

    fun getCategoryForTag(tag: String): TagCategory {
        return when {
            tag.startsWith("@") -> TagCategory.People
            tag.startsWith("#") -> TagCategory.Topics
            else -> TagCategory.Spaces
        }
    }

    fun getCleanTagName(tag: String): String {
        return tag.removePrefix("@").removePrefix("#")
    }

    fun appendPrefix(tag: String, category: TagCategory): String {
        return when (category) {
            TagCategory.People -> "@$tag"
            TagCategory.Topics -> "#$tag"
            TagCategory.Spaces -> tag
        }
    }

    fun filterTagsByCategory(tags: List<String>, category: TagCategory): List<String> {
        return tags.filter { tag -> getCategoryForTag(tag) == category }
    }

    @Composable
    fun getCategoryUiSpec(category: TagCategory): CategoryUiSpec {
        return when (category) {
            TagCategory.Spaces -> CategoryUiSpec(
                iconRes = R.drawable.view_cozy_24px,
                filledIconRes = R.drawable.view_cozy_24px_fill,
                color = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                emptyMessageRes = R.string.spaces_empty_msg,
                descriptionRes = R.string.spaces_desc
            )

            TagCategory.People -> CategoryUiSpec(
                iconRes = R.drawable.groups_24px,
                filledIconRes = R.drawable.groups_24px_fill,
                color = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                emptyMessageRes = R.string.people_empty_msg,
                descriptionRes = R.string.people_desc
            )

            TagCategory.Topics -> CategoryUiSpec(
                iconRes = R.drawable.cards_stack_24px,
                filledIconRes = R.drawable.cards_stack_24px_fill,
                color = MaterialTheme.colorScheme.tertiary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                emptyMessageRes = R.string.topics_empty_msg,
                descriptionRes = R.string.topics_desc
            )
        }
    }

    @Composable
    fun getTagBaseColors(tag: String): Pair<Color, Color> {
        val category = getCategoryForTag(tag)
        val spec = getCategoryUiSpec(category)
        return spec.containerColor to spec.color
    }

    @Composable
    fun getTagInputChipColors(tag: String): SelectableChipColors {
        val (container, content) = getTagBaseColors(tag)
        return InputChipDefaults.inputChipColors(
            containerColor = container,
            labelColor = content,
            leadingIconColor = content,
            trailingIconColor = content,
            selectedContainerColor = container,
            selectedLabelColor = content,
            selectedLeadingIconColor = content,
            selectedTrailingIconColor = content
        )
    }

    @Composable
    fun getTagSuggestionChipColors(tag: String): ChipColors {
        val (container, content) = getTagBaseColors(tag)
        return SuggestionChipDefaults.suggestionChipColors(
            containerColor = container,
            labelColor = content,
            iconContentColor = content
        )
    }
}