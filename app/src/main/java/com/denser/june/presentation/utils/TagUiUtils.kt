package com.denser.june.presentation.utils

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.denser.june.core.domain.model.enums.TagCategory
import com.denser.june.core.utils.TagUtils

object TagUiUtils {

    data class CategoryUiSpec(
        val iconRes: Int,
        val filledIconRes: Int,
        val color: Color,
        val containerColor: Color,
        val emptyMessageRes: Int,
        val descriptionRes: Int
    )

    @Composable
    fun getCategoryUiSpec(category: TagCategory): CategoryUiSpec {
        val (color, containerColor) = when (category) {
            TagCategory.Spaces -> MaterialTheme.colorScheme.onSecondaryContainer to MaterialTheme.colorScheme.secondaryContainer
            TagCategory.People -> MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.primaryContainer
            TagCategory.Topics -> MaterialTheme.colorScheme.onTertiaryContainer to MaterialTheme.colorScheme.tertiaryContainer
        }
        return CategoryUiSpec(
            iconRes = category.iconRes,
            filledIconRes = category.filledIconRes,
            color = color,
            containerColor = containerColor,
            emptyMessageRes = category.emptyMessageRes,
            descriptionRes = category.descriptionRes
        )
    }

    @Composable
    fun getTagBaseColors(tag: String): Pair<Color, Color> {
        val category = TagUtils.getCategoryForTag(tag)
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

