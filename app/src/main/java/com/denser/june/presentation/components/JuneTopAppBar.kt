package com.denser.june.presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

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

@Composable
fun JuneBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    ),
    contentDescription: String = stringResource(R.string.back)
) {
    FilledIconButton(
        onClick = onClick,
        colors = colors,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_back_24px),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun JuneCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    ),
    contentDescription: String = stringResource(R.string.close)
) {
    FilledIconButton(
        onClick = onClick,
        colors = colors,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.close_24px),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun JuneMoreOptionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    ),
    contentDescription: String = stringResource(R.string.options)
) {
    FilledIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        colors = colors,
        modifier = modifier
            .width(32.dp)
            .height(40.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.more_vert_24px),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}