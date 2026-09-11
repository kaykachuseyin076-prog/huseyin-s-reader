package com.example.ui.notebook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HighDensityPrimaryContainer
import com.example.ui.theme.HighDensityPrimaryNavy

/**
 * Modern floating/docked toolbar for the Digital Notebook.
 * Fully optimized for tablet landscape/portrait and phones.
 */
@Composable
fun NotebookToolbar(
    title: String,
    currentPageIndex: Int,
    totalPages: Int,
    currentTool: NotebookTool,
    selectedColor: Color,
    selectedThickness: NotebookPenThickness,
    selectedShapeType: NotebookShapeType,
    stylusOnlyMode: Boolean,
    zoomScale: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelected: (NotebookTool) -> Unit,
    onColorSelected: (Color, Long) -> Unit,
    onThicknessSelected: (NotebookPenThickness) -> Unit,
    onShapeTypeSelected: (NotebookShapeType) -> Unit,
    onStylusOnlyToggled: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomReset: () -> Unit,
    onZoomLevelSelected: (Float) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddPage: () -> Unit,
    onOpenPageManager: () -> Unit,
    onTitleClicked: () -> Unit,
    onChangeTemplateClicked: () -> Unit,
    onDeleteNotebookClicked: () -> Unit,
    onBack: () -> Unit,
    sourceBookTitle: String? = null,
    sourceLocationText: String? = null,
    onNavigateToSource: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var isShapeMenuOpen by remember { mutableStateOf(false) }
    var isThicknessMenuOpen by remember { mutableStateOf(false) }
    var isZoomMenuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // SOURCE BOOK BREADCRUMB & "Kaynağa Git" BANNER
            if (!sourceBookTitle.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
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
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kaynak: $sourceBookTitle" + if (!sourceLocationText.isNullOrBlank()) " • $sourceLocationText" else "",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (onNavigateToSource != null) {
                            FilledTonalButton(
                                onClick = onNavigateToSource,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("go_to_source_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kaynağa Git",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // TOP BAR: Navigation, Title, Quick Actions & Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("notebook_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Notebook Title (Clickable to rename)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTitleClicked)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "İsmi Değiştir",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Page Navigation (< Sayfa X / Y > + Ekle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onPrevPage,
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.size(32.dp).testTag("notebook_prev_page")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Sayfa", modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "${currentPageIndex + 1} / $totalPages",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .clickable(onClick = onOpenPageManager)
                            .padding(horizontal = 6.dp)
                            .testTag("notebook_page_counter")
                    )

                    IconButton(
                        onClick = onNextPage,
                        enabled = currentPageIndex < totalPages - 1,
                        modifier = Modifier.size(32.dp).testTag("notebook_next_page")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Sayfa", modifier = Modifier.size(18.dp))
                    }

                    // Add Page Button
                    IconButton(
                        onClick = onAddPage,
                        modifier = Modifier.size(32.dp).testTag("notebook_add_page_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Yeni Sayfa", modifier = Modifier.size(18.dp))
                    }

                    // Page Thumbnails Grid Button
                    IconButton(
                        onClick = onOpenPageManager,
                        modifier = Modifier.size(32.dp).testTag("notebook_page_grid_button")
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = "Tüm Sayfalar", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Stylus-Only (Palm Rejection) Toggle Button
                Box {
                    FilterChip(
                        selected = stylusOnlyMode,
                        onClick = onStylusOnlyToggled,
                        label = {
                            Text(
                                if (stylusOnlyMode) "Stylus" else "Dokunma",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HighDensityPrimaryContainer,
                            selectedLabelColor = HighDensityPrimaryNavy
                        ),
                        modifier = Modifier.testTag("notebook_stylus_toggle")
                    )
                }

                // More Menu Button
                Box {
                    IconButton(onClick = { isMoreMenuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Daha Fazla")
                    }

                    DropdownMenu(
                        expanded = isMoreMenuOpen,
                        onDismissRequest = { isMoreMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("İsmi Değiştir") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                isMoreMenuOpen = false
                                onTitleClicked()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Şablonu Değiştir") },
                            leadingIcon = { Icon(Icons.Default.Style, contentDescription = null) },
                            onClick = {
                                isMoreMenuOpen = false
                                onChangeTemplateClicked()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sayfa Yönetimi") },
                            leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                            onClick = {
                                isMoreMenuOpen = false
                                onOpenPageManager()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Not Defterini Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                isMoreMenuOpen = false
                                onDeleteNotebookClicked()
                            }
                        )
                    }
                }
            }

            // BOTTOM TOOLBAR: Drawing Tools, Palettes, Undo/Redo & Zoom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Kalem (Pen)
                ToolIconButton(
                    icon = Icons.Default.Brush,
                    label = "Kalem",
                    isSelected = currentTool == NotebookTool.PEN,
                    indicatorColor = selectedColor,
                    onClick = { onToolSelected(NotebookTool.PEN) }
                )

                // Fosforlu Kalem (Highlighter)
                ToolIconButton(
                    icon = Icons.Default.Highlight,
                    label = "Fosforlu",
                    isSelected = currentTool == NotebookTool.HIGHLIGHTER,
                    indicatorColor = selectedColor.copy(alpha = 0.7f),
                    onClick = { onToolSelected(NotebookTool.HIGHLIGHTER) }
                )

                // Silgi (Eraser)
                ToolIconButton(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Silgi",
                    isSelected = currentTool == NotebookTool.ERASER,
                    onClick = { onToolSelected(NotebookTool.ERASER) }
                )

                // Lasso / Seçim
                ToolIconButton(
                    icon = Icons.Default.CropFree,
                    label = "Lasso",
                    isSelected = currentTool == NotebookTool.LASSO,
                    onClick = { onToolSelected(NotebookTool.LASSO) }
                )

                // El / Pan
                ToolIconButton(
                    icon = Icons.Default.PanTool,
                    label = "Kaydır",
                    isSelected = currentTool == NotebookTool.HAND,
                    onClick = { onToolSelected(NotebookTool.HAND) }
                )

                // Metin Kutusu (Text)
                ToolIconButton(
                    icon = Icons.Default.TextFields,
                    label = "Metin",
                    isSelected = currentTool == NotebookTool.TEXT,
                    onClick = { onToolSelected(NotebookTool.TEXT) }
                )

                // Şekiller (Shapes Dropdown)
                Box {
                    ToolIconButton(
                        icon = Icons.Default.Category,
                        label = selectedShapeType.displayName,
                        isSelected = currentTool == NotebookTool.SHAPE,
                        onClick = {
                            onToolSelected(NotebookTool.SHAPE)
                            isShapeMenuOpen = true
                        }
                    )

                    DropdownMenu(
                        expanded = isShapeMenuOpen,
                        onDismissRequest = { isShapeMenuOpen = false }
                    ) {
                        NotebookShapeType.entries.forEach { shape ->
                            DropdownMenuItem(
                                text = { Text(shape.displayName) },
                                onClick = {
                                    onShapeTypeSelected(shape)
                                    onToolSelected(NotebookTool.SHAPE)
                                    isShapeMenuOpen = false
                                }
                            )
                        }
                    }
                }

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

                // COLOR PALETTE (Pen & Highlighter)
                val palette = if (currentTool == NotebookTool.HIGHLIGHTER) {
                    NotebookPalettes.HIGHLIGHTER_COLORS
                } else {
                    NotebookPalettes.PEN_COLORS
                }

                palette.forEach { colorOpt ->
                    val isColorSelected = colorOpt.color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colorOpt.color)
                            .border(
                                width = if (isColorSelected) 3.dp else 1.dp,
                                color = if (isColorSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(colorOpt.color, colorOpt.colorLong) }
                    )
                }

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

                // THICKNESS SELECTOR (İnce, Orta, Kalın)
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { isThicknessMenuOpen = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedThickness.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    DropdownMenu(
                        expanded = isThicknessMenuOpen,
                        onDismissRequest = { isThicknessMenuOpen = false }
                    ) {
                        NotebookPenThickness.entries.forEach { thickness ->
                            DropdownMenuItem(
                                text = { Text(thickness.displayName) },
                                onClick = {
                                    onThicknessSelected(thickness)
                                    isThicknessMenuOpen = false
                                }
                            )
                        }
                    }
                }

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

                // UNDO & REDO
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(34.dp).testTag("notebook_undo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Geri Al",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(34.dp).testTag("notebook_redo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "İleri Al",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

                // ZOOM CONTROLS (%100, %200, %300, etc.)
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { isZoomMenuOpen = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("notebook_zoom_badge")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(zoomScale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    DropdownMenu(
                        expanded = isZoomMenuOpen,
                        onDismissRequest = { isZoomMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("%100 (Normal)") },
                            onClick = {
                                onZoomLevelSelected(1.0f)
                                isZoomMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("%200 (Rahat Yazı)") },
                            onClick = {
                                onZoomLevelSelected(2.0f)
                                isZoomMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("%300 (Detaylı Çizim)") },
                            onClick = {
                                onZoomLevelSelected(3.0f)
                                isZoomMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("%400 (İnce Detaylar)") },
                            onClick = {
                                onZoomLevelSelected(4.0f)
                                isZoomMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sayfaya Sığdır") },
                            onClick = {
                                onZoomReset()
                                isZoomMenuOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    indicatorColor: Color? = null,
    onClick: () -> Unit
) {
    val bg = if (isSelected) HighDensityPrimaryContainer else Color.Transparent
    val tint = if (isSelected) HighDensityPrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("notebook_tool_${label.lowercase()}")
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )
    }
}
