package com.ruby.stream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruby.stream.ui.theme.RubyRadius
import com.ruby.stream.ui.theme.RubySpacing
import com.ruby.stream.ui.theme.RubyType
import com.ruby.stream.ui.theme.TextPrimary

/**
 * Attached, non-standalone status indicator — always sits on top of another
 * component (a card, an image), never appears in its own row. For standalone
 * selectable elements (e.g. Genre filters), use a Chip instead.
 * Covers: Quality (4K/HD), HDR (HDR10/Dolby Vision), and similar short-text
 * metadata labels. One generic composable, since these only differ by text.
 */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RubyRadius.sm))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = RubySpacing.xxs, vertical = 2.dp)
    ) {
        Text(text = text, style = RubyType.Label, color = TextPrimary)
    }
}
