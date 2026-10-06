package com.example.rehabilitationassistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkSlateColorScheme = darkColorScheme(
    primary = MovingBlue,
    background = SlateBackground,
    surface = SlateSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun RehabilitationAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkSlateColorScheme,
        typography = Typography,
        content = content
    )
}