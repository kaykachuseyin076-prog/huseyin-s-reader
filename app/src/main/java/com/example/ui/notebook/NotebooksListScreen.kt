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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.NotebookEntity
import com.example.data.repository.NotebookRepository
import com.example.ui.theme.HighDensityBorderChip
import com.example.ui.theme.HighDensityOnPrimaryContainer
import com.example.ui.theme.HighDensityPrimaryContainer
import com.example.ui.theme.HighDensityPrimaryNavy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notes Home Page (Notlar Ana Sayfası).
 * Displays user's digital notebooks with cover preview, page count, last modified date,
 * and a prominent "+ Yeni Not" button with template selection.
 */
@Composable
fun NotebooksListScreen(
    onNotebookSelected: (NotebookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { NotebookRepository(context) }
    val notebooks by repository.allNotebooks.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var notebookToRename by remember { mutableStateOf<NotebookEntity?>(null) }
    var notebookToDelete by remember { mutableStateOf<NotebookEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Count & New Note Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Not Defterleri",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (notebooks.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HighDensityPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${notebooks.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = HighDensityOnPrimaryContainer
                            )
                        }
                    }
                }

                // Prominent "+ Yeni Not" Button
                Button(
                    onClick = { isCreateDialogOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_new_notebook_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yeni Not", fontWeight = FontWeight.SemiBold)
                }
            }

            if (notebooks.isEmpty()) {
                EmptyNotebooksView(onCreateClick = { isCreateDialogOpen = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notebooks_grid")
                ) {
                    items(
                        items = notebooks,
                        key = { it.id }
                    ) { nb ->
                        NotebookCard(
                            notebook = nb,
                            onClick = { onNotebookSelected(nb) },
                            onRenameClick = { notebookToRename = nb },
                            onDeleteClick = { notebookToDelete = nb }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // CREATE NEW NOTEBOOK DIALOG WITH 8 TEMPLATES
    // ==========================================
    if (isCreateDialogOpen) {
        CreateNotebookTemplateDialog(
            onConfirm = { title, selectedTemplate ->
                scope.launch {
                    val newId = repository.createNotebook(title, selectedTemplate.id)
                    val newNb = repository.getNotebook(newId)
                    isCreateDialogOpen = false
                    if (newNb != null) {
                        onNotebookSelected(newNb)
                    }
                }
            },
            onDismiss = { isCreateDialogOpen = false }
        )
    }

    if (notebookToRename != null) {
        RenameNotebookDialog(
            currentTitle = notebookToRename!!.title,
            onConfirm = { newTitle ->
                scope.launch {
                    repository.renameNotebook(notebookToRename!!.id, newTitle)
                    notebookToRename = null
                }
            },
            onDismiss = { notebookToRename = null }
        )
    }

    if (notebookToDelete != null) {
        DeleteNotebookConfirmDialog(
            notebookTitle = notebookToDelete!!.title,
            onConfirm = {
                scope.launch {
                    repository.deleteNotebook(notebookToDelete!!.id)
                    notebookToDelete = null
                }
            },
            onDismiss = { notebookToDelete = null }
        )
    }
}

/**
 * Notebook card in the grid.
 */
@Composable
fun NotebookCard(
    notebook: NotebookEntity,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val template = NotebookTemplate.fromId(notebook.defaultTemplate)
    val dateStr = remember(notebook.updatedAt) {
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("tr"))
        sdf.format(Date(notebook.updatedAt))
    }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("notebook_card_${notebook.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Notebook Preview Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFF7F8FA))
                    .padding(8.dp)
            ) {
                // Paper mockup inside thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(2.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E6EE), RoundedCornerShape(6.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawNotebookTemplateBackground(this, template, size.width, size.height, false)
                    }
                }

                // Top left template badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HighDensityPrimaryNavy.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = template.title,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Notebook Info (Title, Page Count, Last Modified)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notebook.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menü", modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Yeniden Adlandır") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onRenameClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${notebook.pageCount} Sayfa",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Dialog to create a new notebook with template picker.
 */
@Composable
fun CreateNotebookTemplateDialog(
    onConfirm: (title: String, template: NotebookTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(NotebookTemplate.BLANK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Yeni Not Defteri Oluştur",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Defter Adı (Örn: İslam Felsefesi)") },
                    placeholder = { Text(selectedTemplate.title) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_notebook_title_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Kağıt Şablonu Seçin:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 8 Templates Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp).fillMaxWidth()
                ) {
                    items(NotebookTemplate.entries) { tmpl ->
                        val isSelected = tmpl == selectedTemplate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HighDensityPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTemplate = tmpl }
                                .padding(8.dp)
                                .testTag("template_option_${tmpl.id.lowercase()}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E6EE), RoundedCornerShape(4.dp))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawNotebookTemplateBackground(this, tmpl, size.width, size.height, false)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tmpl.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) HighDensityPrimaryNavy else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { selectedTemplate.title }
                    onConfirm(finalTitle, selectedTemplate)
                },
                modifier = Modifier.testTag("confirm_create_notebook_button")
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun EmptyNotebooksView(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(HighDensityPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = HighDensityPrimaryNavy,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Henüz Not Defteriniz Yok",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Stylus kalem veya parmakla ders çalışmak, serbest çizim yapmak veya kitap notları tutmak için yeni bir defter açın.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .testTag("empty_create_notebook_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("İlk Not Defterini Oluştur")
        }
    }
}
