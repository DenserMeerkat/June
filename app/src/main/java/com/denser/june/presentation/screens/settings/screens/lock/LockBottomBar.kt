package com.denser.june.presentation.screens.settings.screens.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

@Composable
fun LockBottomBar(
    modifier: Modifier = Modifier,
    onLeftClick: (() -> Unit)? = null,
    leftIcon: Int? = null,
    leftText: String? = null,
    onRightClick: (() -> Unit)? = null,
    rightEnabled: Boolean = true,
    rightText: String? = null,
    rightIcon: Int = R.drawable.start_24px
) {
    if (onLeftClick == null && onRightClick == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onLeftClick != null) {
            OutlinedButton(
                onClick = onLeftClick,
                shape = CircleShape,
                contentPadding = if (leftText != null) {
                    PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                } else {
                    PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                }
            ) {
                if (leftIcon != null) {
                    Icon(
                        painter = painterResource(leftIcon),
                        contentDescription = leftText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (leftText != null) {
                    if (leftIcon != null) Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = leftText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (onRightClick != null) {
            Button(
                onClick = onRightClick,
                enabled = rightEnabled,
                shape = CircleShape,
                contentPadding = if (rightText != null) {
                    PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                } else {
                    PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                }
            ) {
                if (rightText != null) {
                    Text(
                        text = rightText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    painter = painterResource(rightIcon),
                    contentDescription = rightText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
