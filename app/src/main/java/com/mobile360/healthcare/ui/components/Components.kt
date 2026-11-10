package com.mobile360.healthcare.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile360.healthcare.R
import com.mobile360.healthcare.ui.theme.AppColors

// ---- Top bars ----

@Composable
fun CareTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}

// Brand bar: hospital mark, app name and trailing actions (for example a notifications button).
@Composable
fun BrandTopBar(modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = AppColors.primaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.primary,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}

// ---- Bottom navigation ----

enum class AppTab(val label: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    APPOINTMENTS(R.string.tab_appointments, Icons.Outlined.CalendarToday, Icons.Filled.CalendarToday),
    RECORDS(R.string.tab_records, Icons.Outlined.Description, Icons.Filled.Description),
    MESSAGES(R.string.tab_messages, Icons.Outlined.Chat, Icons.Filled.Chat),
    PROFILE(R.string.tab_profile, Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun CareBottomBar(selected: AppTab?, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier,
        containerColor = AppColors.surfaceContainerLowest,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        AppTab.entries.forEach { tab ->
            val label = stringResource(tab.label)
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(if (tab == selected) tab.selectedIcon else tab.icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.onTonalContainer,
                    selectedTextColor = AppColors.primary,
                    indicatorColor = AppColors.tonalContainer,
                    unselectedIconColor = AppColors.onSurfaceVariant,
                    unselectedTextColor = AppColors.onSurfaceVariant
                )
            )
        }
    }
}

// ---- Buttons ----

enum class ButtonKind { Primary, Tonal, Outlined }

@Composable
fun CareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val sized = modifier.heightIn(min = 48.dp)
    val content: @Composable RowScope.() -> Unit = {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
    when (kind) {
        ButtonKind.Primary -> Button(
            onClick = onClick, modifier = sized, enabled = enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryContainer, contentColor = Color.White),
            content = content
        )
        ButtonKind.Tonal -> FilledTonalButton(
            onClick = onClick, modifier = sized, enabled = enabled, shape = shape,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = AppColors.tonalContainer, contentColor = AppColors.onTonalContainer),
            content = content
        )
        ButtonKind.Outlined -> OutlinedButton(
            onClick = onClick, modifier = sized, enabled = enabled, shape = shape,
            border = BorderStroke(1.dp, AppColors.buttonOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.onOutlineButton),
            content = content
        )
    }
}

// ---- Cards, rows, sections ----

@Composable
fun CareCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val colors = CardDefaults.cardColors(containerColor = AppColors.surfaceContainerLowest)
    val border = BorderStroke(1.dp, AppColors.outline.copy(alpha = 0.2f))
    val inner: @Composable ColumnScope.() -> Unit = {
        Row(Modifier.height(IntrinsicSize.Min)) {
            if (accent != null) Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(modifier = Modifier.padding(16.dp).weight(1f), content = content)
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = inner)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border, content = inner)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier.fillMaxWidth().then(clickable).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

// ---- Badges and chips ----

enum class BadgeTone { Positive, Neutral, Warning, Negative }

@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        BadgeTone.Positive -> AppColors.primaryFixed to AppColors.onPrimaryFixed
        BadgeTone.Neutral -> AppColors.surfaceContainerHigh to AppColors.onSurfaceVariant
        BadgeTone.Warning -> AppColors.tertiaryFixed to AppColors.onTertiaryFixed
        BadgeTone.Negative -> AppColors.errorContainer to AppColors.onErrorContainer
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
fun CareChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.tonalContainer,
            selectedLabelColor = AppColors.onTonalContainer,
            selectedLeadingIconColor = AppColors.onTonalContainer,
            labelColor = AppColors.chipText
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.buttonOutline.copy(alpha = 0.3f),
            selectedBorderColor = AppColors.tonalContainer
        )
    )
}

// ---- Avatars ----

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() && !it.endsWith(".") }.take(2).joinToString("") { it.first().uppercase() }

@Composable
fun PersonAvatar(@DrawableRes imageRes: Int, name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    if (imageRes != 0) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(AppColors.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(initials(name), style = MaterialTheme.typography.labelLarge, color = AppColors.onSecondaryContainer)
        }
    }
}

// ---- States and banners ----

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.ErrorOutline,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.secondary, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null) CareButton(actionLabel, onAction, kind = ButtonKind.Tonal)
    }
}

@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.WifiOff,
    tone: BadgeTone = BadgeTone.Neutral,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val (bg, fg) = when (tone) {
        BadgeTone.Negative -> AppColors.errorContainer to AppColors.onErrorContainer
        BadgeTone.Warning -> AppColors.tertiaryFixed to AppColors.onTertiaryFixed
        BadgeTone.Positive -> AppColors.primaryFixed to AppColors.onPrimaryFixed
        BadgeTone.Neutral -> AppColors.surfaceContainerHigh to AppColors.onSurfaceVariant
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier, lines: Int = 3) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(lines) {
            Box(
                Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.surfaceContainerHigh)
            )
        }
    }
}

// ---- Text field ----

@Composable
fun CareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.primaryContainer,
            unfocusedBorderColor = AppColors.buttonOutline,
            focusedLabelColor = AppColors.primary
        )
    )
}
