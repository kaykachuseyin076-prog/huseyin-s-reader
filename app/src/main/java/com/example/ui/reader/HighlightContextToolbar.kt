package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Contextual Action Menu when text is selected.
 * Offers 5 color choices, Underline Highlight creation, or Underline + Immediate Post-it Note.
 */
@Composable
fun HighlightContextToolbar(
    selectedText: String,
    selectedColor: HighlightColor,
    onColorSelect: (HighlightColor) -> Unit,
    onHighlightOnly: () -> Unit,
    onHighlightWithNote: () -> Unit,
    onTranslate: () -> Unit,
    onAiHelp: () -> Unit,
    onDiscuss: () -> Unit,
    onDetailedNote: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(max = 480.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = selectedColor.lineColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .testTag("highlight_context_toolbar"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Selected snippet & dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "“$selectedText”",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Vazgeç",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color Selection Palette (Sarı, Turuncu, Yeşil, Mavi, Pembe)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                for (color in HighlightColor.entries) {
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(if (isSelected) 30.dp else 24.dp)
                            .clip(CircleShape)
                            .background(color.chipColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) color.lineColor else Color.Black.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelect(color) }
                            .testTag("color_chip_${color.id.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: [ Vurgula ] and [ Not Ekle ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Highlight only button
                FilledTonalButton(
                    onClick = onHighlightOnly,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_highlight_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = selectedColor.lineColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vurgula",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        fontSize = 12.sp
                    )
                }

                // Highlight with Note button
                FilledTonalButton(
                    onClick = onHighlightWithNote,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("action_highlight_with_note_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = selectedColor.lineColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Not Ekle (Post-it)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        fontSize = 12.sp
                    )
                }

                if (onDetailedNote != null) {
                    FilledTonalButton(
                        onClick = onDetailedNote,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("action_detailed_notebook_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Defter Notu",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // AI Action Buttons: [ Çevir ] [ AI Yardım ] [ Tartış ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Çevir
                FilledTonalButton(
                    onClick = onTranslate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_ai_translate_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Çevir",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        fontSize = 12.sp
                    )
                }

                // AI Yardım
                FilledTonalButton(
                    onClick = onAiHelp,
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("action_ai_help_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI Yardım",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        fontSize = 12.sp
                    )
                }

                // Tartış
                FilledTonalButton(
                    onClick = onDiscuss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_ai_discuss_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tartış",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
