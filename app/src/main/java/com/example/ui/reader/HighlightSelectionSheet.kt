package com.example.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interactive Selection & Highlight Creation Sheet.
 * Allows choosing single word, word groups, or full sentences, selecting 1 of 5 colors,
 * and creating either a clean underline highlight or a highlight with an attached Post-it note.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HighlightSelectionSheet(
    sheetState: SheetState,
    initialSelectedText: String = "",
    paragraphText: String,
    initialStartOffset: Int,
    initialEndOffset: Int,
    onConfirmHighlight: (selectedText: String, startOffset: Int, endOffset: Int, color: HighlightColor, addNoteImmediately: Boolean) -> Unit,
    onTranslate: (selectedText: String) -> Unit = {},
    onAiHelp: (selectedText: String) -> Unit = {},
    onDiscuss: (selectedText: String) -> Unit = {},
    onDetailedNote: ((selectedText: String, startOffset: Int, endOffset: Int, color: HighlightColor) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // Mode of selection: 0 = Exact selection, 1 = Sentence, 2 = Full paragraph
    var selectionMode by remember(initialSelectedText, paragraphText, initialStartOffset, initialEndOffset) {
        mutableIntStateOf(0)
    }

    var selectedColor by remember { mutableStateOf(HighlightColor.ORANGE) }

    // Derive active selection text and offsets
    val activeSelection = remember(selectionMode, initialSelectedText, paragraphText, initialStartOffset, initialEndOffset) {
        when (selectionMode) {
            0 -> {
                // Exact selection
                val text = if (initialSelectedText.isNotBlank()) {
                    initialSelectedText
                } else if (initialStartOffset < initialEndOffset && initialEndOffset <= paragraphText.length) {
                    paragraphText.substring(initialStartOffset, initialEndOffset)
                } else {
                    paragraphText
                }
                Triple(text, initialStartOffset, initialEndOffset)
            }
            1 -> {
                // Sentence
                val range = findSentenceRange(paragraphText, initialStartOffset)
                val text = if (range.first < range.second && range.second <= paragraphText.length) {
                    paragraphText.substring(range.first, range.second)
                } else {
                    paragraphText
                }
                Triple(text, range.first, range.second)
            }
            else -> {
                // Full paragraph
                Triple(paragraphText, 0, paragraphText.length)
            }
        }
    }

    val selectedText = activeSelection.first
    val currentStartOffset = activeSelection.second
    val currentEndOffset = activeSelection.third

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.testTag("highlight_selection_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header: Title & Quote Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = selectedColor.lineColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Metin Vurgula & Post-it Not",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scope toggle chips: [ Seçilen Metin ] [ Tam Cümle ] [ Paragraf ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectionMode == 0,
                    onClick = { selectionMode = 0 },
                    label = { Text("Seçim", fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_scope_selection")
                )
                FilterChip(
                    selected = selectionMode == 1,
                    onClick = { selectionMode = 1 },
                    label = { Text("Cümle", fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_scope_sentence")
                )
                FilterChip(
                    selected = selectionMode == 2,
                    onClick = { selectionMode = 2 },
                    label = { Text("Tüm Paragraf", fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_scope_paragraph")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Text Preview Card
            Surface(
                color = selectedColor.postItBackground,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, selectedColor.postItBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "İşaretlenecek Metin:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF5D4037)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“$selectedText”",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF1B1B1B),
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Picker Row (5 Colors: Sarı, Turuncu, Yeşil, Mavi, Pembe)
            Text(
                text = "Vurgu Rengi",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (color in HighlightColor.entries) {
                    val isSelected = color == selectedColor
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedColor = color }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color.chipColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) color.lineColor else Color.Black.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .testTag("sheet_color_${color.id.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = color.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) color.lineColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gemini AI Actions: [ Çevir ] [ AI Yardım ] [ Tartış ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Çevir
                FilledTonalButton(
                    onClick = {
                        onTranslate(selectedText)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_ai_translate_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Çevir", fontSize = 12.sp)
                }

                // AI Yardım
                FilledTonalButton(
                    onClick = {
                        onAiHelp(selectedText)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("sheet_ai_help_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Yardım", fontSize = 12.sp)
                }

                // Tartış
                FilledTonalButton(
                    onClick = {
                        onDiscuss(selectedText)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_ai_discuss_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tartış", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Vurgula (Alt çizgi)
                FilledTonalButton(
                    onClick = {
                        onConfirmHighlight(selectedText, currentStartOffset, currentEndOffset, selectedColor, false)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_confirm_highlight_button"),
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Post-it Not Ekle
                Button(
                    onClick = {
                        onConfirmHighlight(selectedText, currentStartOffset, currentEndOffset, selectedColor, true)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColor.lineColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("sheet_confirm_note_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Post-it Ekle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (onDetailedNote != null) {
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onDetailedNote(selectedText, currentStartOffset, currentEndOffset, selectedColor)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("sheet_confirm_detailed_notebook_button"),
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
