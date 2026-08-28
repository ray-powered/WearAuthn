package me.henneke.wearauthn.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val WearAuthnColorScheme = ColorScheme(
    primary = WearAuthnPrimary,
    primaryContainer = WearAuthnPrimaryContainer,
    onPrimary = WearAuthnOnPrimary,
    onPrimaryContainer = WearAuthnOnPrimaryContainer,
    secondary = WearAuthnSecondary,
    secondaryContainer = WearAuthnSecondaryContainer,
    onSecondary = WearAuthnOnSecondary,
    onSecondaryContainer = WearAuthnOnSecondaryContainer,
    tertiary = WearAuthnTertiary,
    onTertiary = WearAuthnOnTertiary,
    surfaceContainer = WearAuthnSurfaceContainer,
    surfaceContainerHigh = WearAuthnSurfaceContainerHigh,
    onSurface = WearAuthnOnSurface,
    onSurfaceVariant = WearAuthnOutline,
    outline = WearAuthnOutline,
    outlineVariant = WearAuthnOutlineVariant,
    background = WearAuthnBackground,
    onBackground = WearAuthnOnBackground,
    error = WearAuthnError,
    onError = WearAuthnOnError,
    errorContainer = WearAuthnErrorContainer,
    onErrorContainer = WearAuthnOnErrorContainer
)

@Composable
fun WearAuthnTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearAuthnColorScheme,
        content = content
    )
}
