package me.henneke.wearauthn.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearAuthnColors = Colors(
    primary = WearAuthnPrimary,
    primaryVariant = WearAuthnPrimaryContainer,
    secondary = WearAuthnSecondary,
    secondaryVariant = WearAuthnSecondaryContainer,
    background = WearAuthnBackground,
    surface = WearAuthnSurfaceContainer,
    error = WearAuthnError,
    onPrimary = WearAuthnOnPrimary,
    onSecondary = WearAuthnOnSecondary,
    onBackground = WearAuthnOnBackground,
    onSurface = WearAuthnOnSurface,
    onSurfaceVariant = WearAuthnOutline,
    onError = WearAuthnOnError
)

@Composable
fun WearAuthnTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearAuthnColors,
        content = content
    )
}

