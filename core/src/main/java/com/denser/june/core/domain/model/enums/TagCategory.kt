package com.denser.june.core.domain.model.enums

import com.denser.june.core.R

enum class TagCategory(
    val label: String,
    val singularLabel: String,
    val prefix: String?,
    val iconRes: Int,
    val filledIconRes: Int,
    val emptyMessageRes: Int,
    val descriptionRes: Int
) {
    Spaces(
        label = "Spaces",
        singularLabel = "Space",
        prefix = null,
        iconRes = R.drawable.view_cozy_24px,
        filledIconRes = R.drawable.view_cozy_24px_fill,
        emptyMessageRes = R.string.spaces_empty_msg,
        descriptionRes = R.string.spaces_desc
    ),
    People(
        label = "People",
        singularLabel = "Person",
        prefix = "@",
        iconRes = R.drawable.groups_24px,
        filledIconRes = R.drawable.groups_24px_fill,
        emptyMessageRes = R.string.people_empty_msg,
        descriptionRes = R.string.people_desc
    ),
    Topics(
        label = "Topics",
        singularLabel = "Topic",
        prefix = "#",
        iconRes = R.drawable.cards_stack_24px,
        filledIconRes = R.drawable.cards_stack_24px_fill,
        emptyMessageRes = R.string.topics_empty_msg,
        descriptionRes = R.string.topics_desc
    );

    companion object {
        fun fromTag(tag: String): TagCategory = when {
            tag.startsWith("@") -> People
            tag.startsWith("#") -> Topics
            else -> Spaces
        }
    }
}
