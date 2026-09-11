package com.example.ui.notebook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NotebookPageEntity
import com.example.data.local.entity.NotebookTextBoxEntity
import com.example.ui.theme.HighDensityPrimaryContainer
import com.example.ui.theme.HighDensityPrimaryNavy

/**
 * Dialog to rename the active notebook.
 */
@Composable
fun RenameNotebookDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Not Defterini Yeniden Adlandır") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Defter Adı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("rename_notebook_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("rename_notebook_confirm")
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

/**
 * Dialog to select or change paper template.
 */
@Composable
fun ChangeTemplateDialog(
    currentTemplate: NotebookTemplate,
    onTemplateSelected: (NotebookTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sayfa Şablonunu Değiştir") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(360.dp).fillMaxWidth()
            ) {
                items(NotebookTemplate.entries) { tmpl ->
                    val isSelected = tmpl == currentTemplate
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) HighDensityPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onTemplateSelected(tmpl) }
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Mini Template Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E6EE), RoundedCornerShape(6.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawNotebookTemplateBackground(this, tmpl, size.width, size.height, false)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tmpl.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) HighDensityPrimaryNavy else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

/**
 * Bottom sheet to manage all pages of the notebook (thumbnails, switch, add, delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerBottomSheet(
    sheetState: SheetState,
    pages: List<NotebookPageEntity>,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sayfa Yönetimi (${pages.size} Sayfa)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = onAddPage,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("page_manager_add_page")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yeni Sayfa")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(340.dp).fillMaxWidth()
            ) {
                items(pages) { page ->
                    val isCurrent = page.pageIndex == currentPageIndex
                    val tmpl = NotebookTemplate.fromId(page.template)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) HighDensityPrimaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isCurrent) 2.5.dp else 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onPageSelected(page.pageIndex)
                                onDismiss()
                            }
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E6EE), RoundedCornerShape(6.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawNotebookTemplateBackground(this, tmpl, size.width, size.height, false)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Sayfa ${page.pageIndex + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) HighDensityPrimaryNavy else MaterialTheme.colorScheme.onSurface
                                )

                                if (pages.size > 1) {
                                    IconButton(
                                        onClick = { onDeletePage(page.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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
}

/**
 * Text Box editor dialog.
 */
@Composable
fun TextBoxEditorDialog(
    initialText: String,
    initialFontSize: Float,
    initialIsBold: Boolean,
    onSave: (text: String, fontSize: Float, isBold: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var isBold by remember { mutableStateOf(initialIsBold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Metin Kutusu Düzenle") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Metin") },
                    placeholder = { Text("Buraya notunuzu yazın...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("textbox_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Font Size Options
                    FilterChip(
                        selected = fontSize == 14f,
                        onClick = { fontSize = 14f },
                        label = { Text("Küçük") }
                    )
                    FilterChip(
                        selected = fontSize == 18f,
                        onClick = { fontSize = 18f },
                        label = { Text("Orta") }
                    )
                    FilterChip(
                        selected = fontSize == 24f,
                        onClick = { fontSize = 24f },
                        label = { Text("Büyük") }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Bold Toggle
                    IconButton(
                        onClick = { isBold = !isBold },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isBold) HighDensityPrimaryContainer else Color.Transparent)
                    ) {
                        Icon(
                            Icons.Default.FormatBold,
                            contentDescription = "Kalın",
                            tint = if (isBold) HighDensityPrimaryNavy else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text, fontSize, isBold) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("textbox_save_button")
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Sil", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        }
    )
}

/**
 * Confirm delete notebook dialog.
 */
@Composable
fun DeleteNotebookConfirmDialog(
    notebookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Not Defterini Sil") },
        text = {
            Text("\"$notebookTitle\" adlı not defteri ve içerisindeki bütün sayfalar kalıcı olarak silinecektir. Emin misiniz?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_delete_notebook")
            ) {
                Text("Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
