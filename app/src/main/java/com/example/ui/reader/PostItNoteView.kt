package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.HighlightEntity
import com.example.data.local.entity.NoteEntity

/**
 * Post-it Style Note Modal & Editor
 * Modeled after physical sticky notes with soft shadows, pastel color matching
 * the highlight, and non-blocking, tablet-friendly sizing.
 */
@Composable
fun PostItNoteDialog(
    highlight: HighlightEntity,
    existingNote: NoteEntity?,
    onSaveNote: (String) -> Unit,
    onDeleteNote: () -> Unit,
    onDeleteHighlight: () -> Unit,
    onTranslate: (() -> Unit)? = null,
    onAiHelp: (() -> Unit)? = null,
    onDiscuss: (() -> Unit)? = null,
    onOpenDetailedNote: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val highlightColor = HighlightColor.fromId(highlight.colorId)
    var noteText by remember(existingNote) {
        mutableStateOf(existingNote?.noteText ?: "")
    }
    var isEditing by remember(existingNote) {
        mutableStateOf(existingNote == null || existingNote.noteText.isBlank())
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .padding(20.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            // Post-it Sticky Paper Surface
            Surface(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 440.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.5.dp,
                        color = highlightColor.postItBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Catch clicks inside card */ }
                    .testTag("post_it_card"),
                color = highlightColor.postItBackground,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    // Top Bar: Sticky Note Header & Color Indicator & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Indicator dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(highlightColor.lineColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Post-it Not",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF2C241D)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("post_it_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color(0xFF6B5848),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Highlighted Quote Snippet (Reference from book text)
                    Surface(
                        color = Color.White.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(highlightColor.lineColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "“${highlight.selectedText}”",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Color(0xFF4A3E33),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (onTranslate != null || onAiHelp != null || onDiscuss != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (onTranslate != null) {
                                FilledTonalButton(
                                    onClick = onTranslate,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("post_it_ai_translate_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Çevir", fontSize = 11.5.sp)
                                }
                            }
                            if (onAiHelp != null) {
                                FilledTonalButton(
                                    onClick = onAiHelp,
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .height(34.dp)
                                        .testTag("post_it_ai_help_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("AI Yardım", fontSize = 11.5.sp)
                                }
                            }
                            if (onDiscuss != null) {
                                FilledTonalButton(
                                    onClick = onDiscuss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("post_it_ai_discuss_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Tartış", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Note Input / Content Area
                    if (isEditing) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = {
                                Text(
                                    text = "Bu cümleyle ilgili düşüncenizi veya notunuzu buraya yazın…",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = Color(0xFF8D7B6D)
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .focusRequester(focusRequester)
                                .testTag("post_it_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.7f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                                focusedBorderColor = highlightColor.postItHeaderColor,
                                unfocusedBorderColor = highlightColor.postItBorder.copy(alpha = 0.8f),
                                cursorColor = highlightColor.lineColor,
                                focusedTextColor = Color(0xFF1F1812),
                                unfocusedTextColor = Color(0xFF1F1812)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                        )
                    } else {
                        // Display Mode (Read Note)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clickable { isEditing = true }
                                .padding(2.dp),
                            color = Color.White.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = noteText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = Color(0xFF1F1812),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional "Gelişmiş Defter Notu Aç" Button
                    if (onOpenDetailedNote != null) {
                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                                onOpenDetailedNote()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("post_it_open_detailed_note_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gelişmiş Defter Notunda Aç",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Delete actions (Notu Sil or İşareti Sil)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Delete Highlight Button
                            IconButton(
                                onClick = {
                                    onDeleteHighlight()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("post_it_delete_highlight_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Vurguyu ve Notu Sil",
                                    tint = Color(0xFFB71C1C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // If note exists, also provide "Notu Sil" button
                            if (existingNote != null && existingNote.noteText.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        onDeleteNote()
                                        onDismiss()
                                    },
                                    modifier = Modifier.testTag("post_it_delete_note_button")
                                ) {
                                    Text(
                                        text = "Notu Sil",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color(0xFF8E24AA),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Right side: Edit / Save Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isEditing && existingNote != null && existingNote.noteText.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { isEditing = true },
                                    modifier = Modifier.testTag("post_it_edit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF3E2723)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Düzenle",
                                        color = Color(0xFF3E2723),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onSaveNote(noteText)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = highlightColor.lineColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("post_it_save_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kaydet",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
