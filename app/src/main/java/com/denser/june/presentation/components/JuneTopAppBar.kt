package com.denser.june.presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class JuneAppBarType {
    Small,
    CenterAligned,
    Medium,
    Large
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    type: JuneAppBarType = JuneAppBarType.Small,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets.add(
        WindowInsets(left = 8.dp, right = 8.dp)
    ),
) {
    when (type) {
        JuneAppBarType.Small -> {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior,
                windowInsets = windowInsets,
                modifier = modifier
            )
        }

        JuneAppBarType.CenterAligned -> {
            CenterAlignedTopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior,
                windowInsets = windowInsets,
                modifier = modifier
            )
        }

        JuneAppBarType.Medium -> {
            MediumTopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior,
                windowInsets = windowInsets,
                modifier = modifier
            )
        }

        JuneAppBarType.Large -> {
            LargeTopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior,
                windowInsets = windowInsets,
                modifier = modifier
            )
        }
    }
}