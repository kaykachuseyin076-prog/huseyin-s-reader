package com.example.ui.reader.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiRepository
import com.example.data.local.dao.DiscussionDao
import com.example.data.local.entity.DiscussionEntity
import com.example.data.local.entity.DiscussionMessageEntity
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionChatSheet(
    sheetState: SheetState,
    selectedText: String,
    highlightId: String?,
    bookId: String,
    discussionDao: DiscussionDao,
    geminiRepository: GeminiRepository,
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
                .testTag("discussion_side_panel"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            DiscussionChatContent(
                selectedText = selectedText,
                highlightId = highlightId,
                bookId = bookId,
                discussionDao = discussionDao,
                geminiRepository = geminiRepository,
                onOpenSettings = onOpenSettings,
                onDismiss = onDismiss
            )
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = null,
            modifier = modifier.fillMaxHeight(0.92f).testTag("discussion_chat_sheet")
        ) {
            DiscussionChatContent(
                selectedText = selectedText,
                highlightId = highlightId,
                bookId = bookId,
                discussionDao = discussionDao,
                geminiRepository = geminiRepository,
                onOpenSettings = onOpenSettings,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun DiscussionChatContent(
    selectedText: String,
    highlightId: String?,
    bookId: String,
    discussionDao: DiscussionDao,
    geminiRepository: GeminiRepository,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var discussionId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<DiscussionMessageEntity>>(emptyList()) }
    var inputMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load existing discussion from Room if available, otherwise create new
    LaunchedEffect(highlightId, selectedText) {
        val existing = discussionDao.getDiscussionByHighlightOrText(highlightId, bookId, selectedText)

        if (existing != null && existing.messages.isNotEmpty()) {
            discussionId = existing.discussion.id
            messages = existing.messages.sortedBy { it.timestamp }
        } else {
            // New discussion session
            val newId = existing?.discussion?.id ?: UUID.randomUUID().toString()
            if (existing == null) {
                val newDiscussion = DiscussionEntity(
                    id = newId,
                    highlightId = highlightId,
                    bookId = bookId,
                    selectedText = selectedText
                )
                discussionDao.insertDiscussion(newDiscussion)
            }
            discussionId = newId
            messages = emptyList()

            // Automatically generate opening discussion response from Gemini
            isSending = true
            errorMessage = null
            coroutineScope.launch {
                val initialResult = geminiRepository.startDiscussion(selectedText)
                initialResult.onSuccess { openingText ->
                    val assistantMsg = DiscussionMessageEntity(
                        id = UUID.randomUUID().toString(),
                        discussionId = newId,
                        role = "assistant",
                        content = openingText,
                        timestamp = System.currentTimeMillis()
                    )
                    discussionDao.insertMessage(assistantMsg)
                    messages = listOf(assistantMsg)
                    isSending = false
                }.onFailure { error ->
                    errorMessage = error.message ?: "Gemini ile bağlantı kurulamadı."
                    isSending = false
                }
            }
        }
    }

    // Scroll to bottom whenever messages change
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val currentInput = inputMessage.trim()
        val dId = discussionId ?: return
        if (currentInput.isBlank() || isSending) return

        inputMessage = ""
        errorMessage = null
        isSending = true

        coroutineScope.launch {
            // 1. Insert user message locally
            val userMsg = DiscussionMessageEntity(
                id = UUID.randomUUID().toString(),
                discussionId = dId,
                role = "user",
                content = currentInput,
                timestamp = System.currentTimeMillis()
            )
            discussionDao.insertMessage(userMsg)
            messages = messages + userMsg

            // 2. Prepare conversation history for Gemini
            val history = messages.dropLast(1).map { it.role to it.content }

            // 3. Call Gemini API
            val result = geminiRepository.continueDiscussion(
                selectedText = selectedText,
                history = history,
                newMessage = currentInput
            )

            result.onSuccess { replyText ->
                val assistantMsg = DiscussionMessageEntity(
                    id = UUID.randomUUID().toString(),
                    discussionId = dId,
                    role = "assistant",
                    content = replyText,
                    timestamp = System.currentTimeMillis()
                )
                discussionDao.insertMessage(assistantMsg)
                discussionDao.updateTimestamp(dId)
                messages = messages + assistantMsg
                isSending = false
            }.onFailure { error ->
                errorMessage = error.message ?: "Gemini yanıt veremedi."
                isSending = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar / Sheet Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tartış & Sohbet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "Gemini ile metin üzerine konuşun",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("discussion_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }
        }

        // Pinned Selected Passage Box
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = "Tartışılan Metin:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "“$selectedText”",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && !isSending && errorMessage == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gemini ile metin üzerine tartışın!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Örn: 'Bu düşünceye karşıt görüşler nelerdir?'",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }

            if (isSending) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini yazıyor…",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (errorMessage!!.contains("Ayarlar")) {
                                    FilledTonalButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenSettings()
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ayarları Aç", fontSize = 11.sp)
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            if (messages.isNotEmpty()) {
                                                val lastUser = messages.lastOrNull { it.role == "user" }
                                                if (lastUser != null) {
                                                    inputMessage = lastUser.content
                                                    sendMessage()
                                                }
                                            } else {
                                                // Retry startDiscussion
                                                isSending = true
                                                errorMessage = null
                                                coroutineScope.launch {
                                                    geminiRepository.startDiscussion(selectedText).onSuccess { op ->
                                                        val dId = discussionId ?: return@launch
                                                        val aMsg = DiscussionMessageEntity(
                                                            id = UUID.randomUUID().toString(),
                                                            discussionId = dId,
                                                            role = "assistant",
                                                            content = op,
                                                            timestamp = System.currentTimeMillis()
                                                        )
                                                        discussionDao.insertMessage(aMsg)
                                                        messages = listOf(aMsg)
                                                        isSending = false
                                                    }.onFailure { err ->
                                                        errorMessage = err.message
                                                        isSending = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tekrar Dene", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Bir mesaj yazın…", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("discussion_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() })
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputMessage.isNotBlank() && !isSending) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(enabled = inputMessage.isNotBlank() && !isSending) {
                            sendMessage()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Gönder",
                        tint = if (inputMessage.isNotBlank() && !isSending) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: DiscussionMessageEntity) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isUser) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                } else {
                    MarkdownText(
                        markdown = message.content,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        lineHeightSp = 19f
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
