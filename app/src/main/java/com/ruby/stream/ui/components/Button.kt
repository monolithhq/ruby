package com.ruby.stream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ruby.stream.ui.theme.Crimson
import com.ruby.stream.ui.theme.CrimsonPressed
import com.ruby.stream.ui.theme.RubyRadius
import com.ruby.stream.ui.theme.RubySpacing
import com.ruby.stream.ui.theme.RubyTouchTarget
import com.ruby.stream.ui.theme.RubyType
import com.ruby.stream.ui.theme.Surface2
import com.ruby.stream.ui.theme.TextDisabled
import com.ruby.stream.ui.theme.TextPrimary

enum class RubyButtonVariant { Primary, Secondary, Tertiary }

@Composable
fun RubyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RubyButtonVariant = RubyButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(RubyRadius.sm)
    val isClickable = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val content: @Composable () -> Unit = {
        Row {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = when (variant) {
                        RubyButtonVariant.Primary -> TextPrimary
                        else -> Crimson
                    }
                )
            } else {
                leadingIcon?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(text = text, style = RubyType.Label)
            }
        }
    }

    when (variant) {
        RubyButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = RubyTouchTarget.minimum),
            enabled = isClickable,
            shape = shape,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPressed) CrimsonPressed else Crimson,
                contentColor = TextPrimary,
                disabledContainerColor = Surface2,
                disabledContentColor = TextDisabled
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = RubySpacing.md,
                vertical = RubySpacing.xs
            )
        ) { content() }

        RubyButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = RubyTouchTarget.minimum),
            enabled = isClickable,
            shape = shape,
            interactionSource = interactionSource,
            border = BorderStroke(1.dp, if (isClickable) Crimson else TextDisabled),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isPressed) CrimsonPressed else Crimson,
                disabledContentColor = TextDisabled
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = RubySpacing.md,
                vertical = RubySpacing.xs
            )
        ) { content() }

        RubyButtonVariant.Tertiary -> TextButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = RubyTouchTarget.minimum),
            enabled = isClickable,
            shape = shape,
            interactionSource = interactionSource,
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isPressed) Crimson else TextPrimary,
                disabledContentColor = TextDisabled
            )
        ) { content() }
    }
}

enum class RubyIconButtonVariant { Standard, OnMedia }

@Composable
fun RubyIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RubyIconButtonVariant = RubyIconButtonVariant.Standard,
    enabled: Boolean = true
) {
    val bgColor = when (variant) {
        RubyIconButtonVariant.Standard -> Color.Transparent
        RubyIconButtonVariant.OnMedia -> Color.Black.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier
            .size(RubyTouchTarget.minimum)
            .padding(RubySpacing.xxs),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(RubyTouchTarget.minimum - RubySpacing.xxs)
                .background(bgColor, CircleShape)
        )
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) TextPrimary else TextDisabled
            )
        }
    }
}
