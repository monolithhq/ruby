package com.ruby.stream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruby.stream.ui.theme.Crimson
import com.ruby.stream.ui.theme.RubyDimens
import com.ruby.stream.ui.theme.RubySpacing
import com.ruby.stream.ui.theme.RubyType
import com.ruby.stream.ui.theme.TextPrimary
import com.ruby.stream.ui.theme.TextSecondary

/**
 * Standalone, interactive filter element — sits in its own horizontal row,
 * independent of any specific piece of content. Used for Genre filters.
 * For attached, non-standalone status indicators (Quality, HDR), use Badge
 * instead. Selection state is owned by the caller (e.g. a filter row
 * tracking which genres are active), not by Chip itself, so multiple chips
 * can be coordinated as a group.
 */
@Composable
fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(percent = 50)
    val backgroundColor = if (selected) Crimson else Color.Transparent

    Box(
        modifier = modifier
            .height(RubyDimens.ChipHeight)
            .clip(shape)
            .background(backgroundColor)
            .let { base ->
                if (!selected) base.border(BorderStroke(1.dp, TextSecondary), shape) else base
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = RubySpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = RubyType.Label,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}
