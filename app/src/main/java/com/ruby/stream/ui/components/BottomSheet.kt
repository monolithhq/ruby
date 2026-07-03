package com.ruby.stream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruby.stream.ui.theme.Crimson
import com.ruby.stream.ui.theme.RubySpacing
import com.ruby.stream.ui.theme.RubyType
import com.ruby.stream.ui.theme.Surface2
import com.ruby.stream.ui.theme.TextPrimary
import com.ruby.stream.ui.theme.TextSecondary
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Generic "pick one from a list" bottom sheet — covers Quality, Subtitles,
 * and Audio selection from the Video Player's Settings Overlay (one
 * component instead of three near-duplicates, since all three share the
 * same interaction: show options, mark the current selection, close on pick).
 * Selected option is marked with a trailing checkmark (Netflix-inspired
 * pattern — see Section 7, "Familiar Interaction, Original Identity").
 */
data class SelectionOption(
    val id: String,
    val title: String,
    val subtitle: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RubySelectionSheet(
    title: String,
    options: List<SelectionOption>,
    selectedId: String?,
    onSelect: (SelectionOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface2
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = RubyType.H3,
                color = TextPrimary,
                modifier = Modifier.padding(
                    horizontal = RubySpacing.md,
                    vertical = RubySpacing.sm
                )
            )
            LazyColumn {
                items(options, key = { it.id }) { option ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isSelected = option.id == selectedId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelect(option) }
                            )
                            .padding(horizontal = RubySpacing.md, vertical = RubySpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = RubyType.Body,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                            if (option.subtitle != null) {
                                Text(
                                    text = option.subtitle,
                                    style = RubyType.Caption,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = Crimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
