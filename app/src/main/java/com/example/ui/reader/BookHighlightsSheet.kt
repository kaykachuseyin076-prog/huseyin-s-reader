package com.example.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HighlightWithNote
import com.example.data.local.entity.NotebookEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Overview sheet of all Notes, Highlights, Post-it Notes, and Handwriting pages for the current book.
 * Allows direct navigation, creating linked notebooks, opening post-it notes, and managing ink notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookHighlightsSheet(
    sheetState: SheetState,
    highlights: List<HighlightWithNote>,
    format: String,
    bookTitle: String = "Kitap",
    currentPage: Int = 0,
    notebooks: List<NotebookEntity> = emptyList(),
    inkPages: List<Int> = emptyList(),
    onCreateNotebookForCurrentPage: (() -> Unit)? = null,
    onSelectNotebook: ((NotebookEntity) -> Unit)? = null,
    onSelectHighlight: (HighlightWithNote) -> Unit,
    onOpenDetailedNoteForHighlight: ((HighlightWithNote) -> Unit)? = null,
    onNavigateToPage: ((Int) -> Unit)? = null,
    onDeleteHighlight: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val entityName = if (format.equals("PDF", ignoreCase = true)) "Sayfa" else "Bölüm"
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var filterOnlyNotes by remember { mutableStateOf(false) }

    val filteredHighlights = remember(highlights, filterOnlyNotes) {
        if (filterOnlyNotes) {
            highlights.filter { it.hasNote }
        } else {
            highlights
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.testTag("book_highlights_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kitap Notları & Vurgular",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$bookTitle • $entityName ${currentPage + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCreateNotebookForCurrentPage != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onCreateNotebookForCurrentPage()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("sheet_add_note_to_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Not Ekle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Row: Defterler | Vurgular | El Yazısı
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Defterler (${notebooks.size})",
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Vurgular (${highlights.size})",
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.StickyNote2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            text = "El Yazısı (${inkPages.size})",
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.Gesture,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: NOTEBOOKS LINKED TO THIS BOOK
                    if (notebooks.isEmpty()) {
                        EmptyStateBox(
                            icon = Icons.Default.MenuBook,
                            title = "Bu kitaba bağlı henüz not defteri yok.",
                            subtitle = "Bu sayfaya veya kitaba ait yeni bir çalışma defteri oluşturmak için 'Not Ekle' butonuna basabilirsiniz.",
                            actionLabel = "Bu Sayfa İçin Not Defteri Başlat",
                            onAction = {
                                onDismiss()
                                onCreateNotebookForCurrentPage?.invoke()
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notebooks, key = { it.id }) { notebook ->
                                val dateStr = remember(notebook.updatedAt) {
                                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(notebook.updatedAt))
                                }
                                val locStr = if (notebook.pdfPage != null) {
                                    "Sayfa ${notebook.pdfPage + 1}"
                                } else if (!notebook.chapterTitle.isNullOrBlank()) {
                                    notebook.chapterTitle
                                } else if (notebook.chapterIndex != null) {
                                    "Bölüm ${notebook.chapterIndex + 1}"
                                } else {
                                    "Kitap Geneli"
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onSelectNotebook?.invoke(notebook)
                                        }
                                        .testTag("notebook_item_${notebook.id}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Default.MenuBook,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = notebook.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = locStr,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (!notebook.sourceSnippet.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "“${notebook.sourceSnippet}”",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontStyle = FontStyle.Italic,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Son düzenleme: $dateStr • ${notebook.pageCount} sayfa",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Aç",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: HIGHLIGHTS & POST-ITS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = filterOnlyNotes,
                            onClick = { filterOnlyNotes = !filterOnlyNotes },
                            label = { Text("Yalnızca Post-it Notlular", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )

                        Text(
                            text = "${filteredHighlights.size} kayıt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredHighlights.isEmpty()) {
                        EmptyStateBox(
                            icon = Icons.Default.Bookmark,
                            title = if (filterOnlyNotes) "Post-it notu içeren vurgu bulunamadı." else "Henüz vurgu veya not eklenmedi.",
                            subtitle = "Bir cümlenin üzerine basılı tutarak vurgulayabilir ve Post-it not ekleyebilirsiniz."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredHighlights, key = { it.highlight.id }) { item ->
                                val hl = item.highlight
                                val color = HighlightColor.fromId(hl.colorId)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = color.postItBackground,
                                    border = BorderStroke(1.dp, color.postItBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("highlight_item_${hl.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header Row: Location & Actions
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(color.lineColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "$entityName ${hl.chapterOrPageIndex + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF424242)
                                                )
                                                if (item.hasNote) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = color.lineColor.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.StickyNote2,
                                                                contentDescription = null,
                                                                tint = color.lineColor,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "Post-it",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                ),
                                                                color = color.lineColor
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { onDeleteHighlight(hl.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Sil",
                                                    tint = Color(0xFFC62828),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Highlighted text quote
                                        Text(
                                            text = "“${hl.selectedText}”",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontStyle = FontStyle.Italic,
                                                fontSize = 13.sp,
                                                lineHeight = 17.sp
                                            ),
                                            color = Color(0xFF212121),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.clickable { onSelectHighlight(item) }
                                        )

                                        // Attached note content if present
                                        if (item.hasNote && !item.note?.noteText.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                color = Color.White.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSelectHighlight(item) }
                                            ) {
                                                Row(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = item.note?.noteText ?: "",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        color = Color(0xFF1B1B1B),
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Action buttons: [ Sayfaya Git ] & [ Gelişmiş Not Aç / Oluştur ]
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { onSelectHighlight(item) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Sayfaya Git", fontSize = 11.sp)
                                            }

                                            if (onOpenDetailedNoteForHighlight != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                FilledTonalButton(
                                                    onClick = {
                                                        onDismiss()
                                                        onOpenDetailedNoteForHighlight(item)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Draw,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Defter Notu Aç", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: HANDWRITING / INK PAGES
                    if (inkPages.isEmpty()) {
                        EmptyStateBox(
                            icon = Icons.Default.Gesture,
                            title = "Bu kitapta henüz el yazısı çizimi yok.",
                            subtitle = "Tablet kalemi veya çizim aracını seçerek dilediğiniz sayfa üzerine not alabilir ve çizim yapabilirsiniz."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(inkPages) { pageIndex ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onNavigateToPage?.invoke(pageIndex)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Gesture,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "$entityName ${pageIndex + 1}",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "El yazısı çizimleri ve notlar mevcut",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                onDismiss()
                                                onNavigateToPage?.invoke(pageIndex)
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Sayfaya Git", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = actionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}
