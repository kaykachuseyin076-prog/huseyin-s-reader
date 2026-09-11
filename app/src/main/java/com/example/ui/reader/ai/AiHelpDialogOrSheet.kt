package com.example.ui.reader.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiRepository
import kotlinx.coroutines.launch

enum class AiHelpMode(val label: String, val icon: ImageVector) {
    EXPLAIN("Açıkla", Icons.Default.Psychology),
    SUMMARIZE("Özetle", Icons.Default.ShortText),
    SIMPLIFY("Basitleştir", Icons.Default.TextSnippet),
    KEY_POINTS("Önemli Noktalar", Icons.Default.List),
    CONTEXT("Bağlam", Icons.Default.HistoryEdu),
    CUSTOM("Soru", Icons.Default.AutoAwesome)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHelpSheet(
    sheetState: SheetState,
    selectedText: String,
    bookTitle: String? = null,
    geminiRepository: GeminiRepository,
    onSaveAsPostItNote: (noteText: String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    isSidePanel: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isSidePanel) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .width(420.dp)
                .shadow(12.dp)
                .testTag("ai_help_side_panel"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            AiHelpContent(
                selectedText = selectedText,
                bookTitle = bookTitle,
                geminiRepository = geminiRepository,
                onSaveAsPostItNote = onSaveAsPostItNote,
                onOpenSettings = onOpenSettings,
                onDismiss = onDismiss,
                isSidePanel = true
            )
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = null,
            modifier = modifier.testTag("ai_help_sheet")
        ) {
            AiHelpContent(
                selectedText = selectedText,
                bookTitle = bookTitle,
                geminiRepository = geminiRepository,
                onSaveAsPostItNote = onSaveAsPostItNote,
                onOpenSettings = onOpenSettings,
                onDismiss = onDismiss,
                isSidePanel = false
            )
        }
    }
}

@Composable
fun AiHelpContent(
    selectedText: String,
    bookTitle: String? = null,
    geminiRepository: GeminiRepository,
    onSaveAsPostItNote: (noteText: String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    isSidePanel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeMode by remember { mutableStateOf(AiHelpMode.EXPLAIN) }
    var customQuestion by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCopied by remember { mutableStateOf(false) }

    fun executeQuery(mode: AiHelpMode, question: String = "") {
        isLoading = true
        errorMessage = null
        isCopied = false
        activeMode = mode
        coroutineScope.launch {
            val result = when (mode) {
                AiHelpMode.EXPLAIN -> geminiRepository.explain(selectedText)
                AiHelpMode.SUMMARIZE -> geminiRepository.summarize(selectedText)
                AiHelpMode.SIMPLIFY -> geminiRepository.simplify(selectedText)
                AiHelpMode.KEY_POINTS -> geminiRepository.extractKeyPoints(selectedText)
                AiHelpMode.CONTEXT -> geminiRepository.getContextAnalysis(selectedText, bookTitle)
                AiHelpMode.CUSTOM -> {
                    if (question.isNotBlank()) {
                        geminiRepository.askCustomQuestion(selectedText, question)
                    } else {
                        Result.failure(Exception("Lütfen bir soru yazın."))
                    }
                }
            }
            result.onSuccess { text ->
                responseText = text
                isLoading = false
            }.onFailure { error ->
                errorMessage = error.message ?: "İşlem tamamlanamadı."
                isLoading = false
            }
        }
    }

    // Auto-trigger default mode (Explain) on launch
    LaunchedEffect(Unit) {
        executeQuery(AiHelpMode.EXPLAIN)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Yardım",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gemini ile metni anla ve analiz et",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("ai_help_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Text Preview Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SEÇİLEN METİN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"$selectedText\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Selection Pills
        Text(
            text = "Hızlı İşlemler",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val modes = listOf(
                AiHelpMode.EXPLAIN,
                AiHelpMode.SUMMARIZE,
                AiHelpMode.SIMPLIFY,
                AiHelpMode.KEY_POINTS,
                AiHelpMode.CONTEXT
            )
            items(modes) { mode ->
                val isSelected = activeMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            if (!isLoading) {
                                executeQuery(mode)
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Output Card with Markdown rendering
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                when {
                    isLoading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Gemini yanıt hazırlıyor…",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (errorMessage!!.contains("Ayarlar")) {
                                    FilledTonalButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenSettings()
                                        }
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ayarları Aç")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            executeQuery(activeMode, customQuestion)
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tekrar Dene")
                                    }
                                }
                            }
                        }
                    }

                    responseText != null -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (activeMode == AiHelpMode.CUSTOM) "Cevap" else "${activeMode.label} Sonucu",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            MarkdownText(
                                markdown = responseText!!,
                                textColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = "Seçilen metinle ilgili işlem seçin veya aşağıdan bir soru sorun.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Question Input Area: [ Sorunuzu yazın... ] [Gönder]
        Text(
            text = "Kendi Sorunuz",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customQuestion,
                onValueChange = { customQuestion = it },
                placeholder = { Text("Sorunuzu yazın...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_question_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (customQuestion.isNotBlank() && !isLoading) {
                        val q = customQuestion
                        customQuestion = ""
                        executeQuery(AiHelpMode.CUSTOM, q)
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = {
                    if (customQuestion.isNotBlank() && !isLoading) {
                        val q = customQuestion
                        customQuestion = ""
                        executeQuery(AiHelpMode.CUSTOM, q)
                    }
                },
                enabled = customQuestion.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("custom_question_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gönder", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gönder", fontSize = 12.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Kopyala + Post-it Not Olarak Kaydet
        AnimatedVisibility(visible = responseText != null && !isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy
                OutlinedButton(
                    onClick = {
                        responseText?.let { t ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AI Yanıtı", t)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_help_copy_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isCopied) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCopied) "Kopyalandı" else "Kopyala",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Save as Post-it note
                Button(
                    onClick = {
                        responseText?.let { t ->
                            onSaveAsPostItNote(t)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("ai_help_save_post_it_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Post-it Ekle",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
