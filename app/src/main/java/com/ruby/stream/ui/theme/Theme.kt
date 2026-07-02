package com.ruby.stream.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Ruby is dark-theme-only by design (matches brand identity — no light theme planned for v1)
private val RubyColorScheme = darkColorScheme(
    primary = Crimson,
    onPrimary = TextPrimary,
    background = Black,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    error = StatusError,
    onError = TextPrimary,
    outline = Divider
)

@Composable
fun RubyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RubyColorScheme,
        typography = RubyMaterialTypography,
        content = content
    )
}
