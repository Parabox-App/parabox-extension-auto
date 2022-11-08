package com.ojhdtapp.parabox.extension.auto.ui.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PreferencesCategory(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier
            .padding(24.dp, 8.dp)
            .fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (value: Boolean) -> Unit
) {
    val textColor by animateColorAsState(targetValue = if(enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
    Row(
        modifier = modifier
            .clickable {
                if (enabled) {
                    onCheckedChange(!checked)
                }
            }
            .padding(24.dp, 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                color = textColor
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
        Spacer(modifier = Modifier.width(48.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun NormalPreference(
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    warning: Boolean = false,
    enabled: Boolean = true,
    roundedCorner: Boolean = false,
    horizontalPadding: Dp = 24.dp,
    onLeadingIconClick: (() -> Unit)? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
    val titleTextColor by animateColorAsState(
        targetValue = when {
            warning -> MaterialTheme.colorScheme.error
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            !enabled -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
    val subTitleTextColor by animateColorAsState(
        targetValue = when {
            warning -> MaterialTheme.colorScheme.error
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            !enabled -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(if (roundedCorner) 24.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (onLeadingIconClick != null && leadingIcon != null) {
                Row(
                    modifier = Modifier
                        .clickable { if (enabled) onLeadingIconClick() }
                        .padding(start = horizontalPadding, top = 16.dp, bottom = 16.dp)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(15.dp))
                    Divider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clickable { if (enabled) onClick() }
                    .padding(
                        start = if (onLeadingIconClick != null && leadingIcon != null) 16.dp else horizontalPadding,
                        end = if (onTrailingIconClick != null && trailingIcon != null) 16.dp else horizontalPadding,
                        top = 16.dp,
                        bottom = 16.dp
                    )
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null && onLeadingIconClick == null) {
                    Box(
                        modifier = Modifier.padding(end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leadingIcon()
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = titleTextColor,
                        fontSize = 20.sp
                    )
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = subTitleTextColor
                        )
                    }
                }
                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.padding(start = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingIcon()
                    }
                }
            }
            if (onTrailingIconClick != null && trailingIcon != null) {
                Row(
                    modifier = Modifier
                        .clickable { if (enabled) onTrailingIconClick() }
                        .padding(end = horizontalPadding, top = 16.dp, bottom = 16.dp)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    trailingIcon()
                }
            }
        }
    }
}