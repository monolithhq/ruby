package com.ruby.stream.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruby.stream.ui.theme.RubySpacing
import com.ruby.stream.ui.theme.RubyType
import com.ruby.stream.ui.theme.TextPrimary
import com.ruby.stream.ui.theme.TextSecondary

/**
 * Generic reusable empty state — used for empty Watchlist, empty Downloads,
 * empty Search results, no add-ons installed, etc. One component, thin
 * call-sites differing only in icon/text/action, matching the pattern used
 * throughout Stage 2B (RubyMediaCard, RubySelectionSheet, StatusChip).
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RubySpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.size(RubySpacing.md))
        Text(
            text = title,
            style = RubyType.H3,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Spacer(Modifier.size(RubySpacing.xs))
            Text(
                text = message,
                style = RubyType.Body,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.size(RubySpacing.lg))
            RubyButton(
                text = actionLabel,
                onClick = onAction,
                variant = RubyButtonVariant.Secondary
            )
        }
    }
}
