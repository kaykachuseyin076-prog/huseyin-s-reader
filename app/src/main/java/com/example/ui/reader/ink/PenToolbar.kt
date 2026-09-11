package com.example.ui.reader.ink

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact, distraction-free floating Pen Toolbar for handwriting and drawing.
 * Supports Pen, Highlighter, Stroke Eraser, Undo, Redo, Color Picker, Thickness Picker,
 * and Stylus/Finger palm-rejection mode toggle.
 */
@Composable
fun PenToolbar(
    activeTool: InkToolType,
    selectedColor: Color,
    selectedThickness: PenThickness,
    canUndo: Boolean,
    canRedo: Boolean,
    stylusOnlyMode: Boolean,
    onSelectTool: (InkToolType) -> Unit,
    onSelectColor: (Color) -> Unit,
    onSelectThickness: (PenThickness) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleStylusOnly: () -> Unit,
    onClosePenMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isColorPaletteOpen by remember { mutableStateOf(false) }
    var isThicknessMenuOpen by remember { mutableStateOf(false) }

    val currentPalette = if (activeTool == InkToolType.HIGHLIGHTER) {
        InkPalettes.HIGHLIGHTER_COLORS
    } else {
        InkPalettes.PEN_COLORS
    }

    Column(
        modifier = modifier.testTag("pen_toolbar_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sub-bar for Color Palette (appears when color dot is clicked)
        AnimatedVisibility(
            visible = isColorPaletteOpen && activeTool != InkToolType.ERASER,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .testTag("ink_color_palette")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (opt in currentPalette) {
                        val isSelected = selectedColor == opt.color
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(opt.color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onSelectColor(opt.color)
                                    isColorPaletteOpen = false
                                }
                                .testTag("ink_color_item_${opt.id}")
                        )
                    }
                }
            }
        }

        // Sub-bar for Thickness Selection (appears when thickness icon is clicked)
        AnimatedVisibility(
            visible = isThicknessMenuOpen && activeTool != InkToolType.ERASER,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .testTag("ink_thickness_menu")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (th in PenThickness.entries) {
                        val isSelected = selectedThickness == th
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .clickable {
                                    onSelectThickness(th)
                                    isThicknessMenuOpen = false
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("ink_thickness_${th.name}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(when (th) {
                                            PenThickness.THIN -> 2.dp
                                            PenThickness.MEDIUM -> 4.dp
                                            PenThickness.THICK -> 7.dp
                                        })
                                        .background(selectedColor, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = th.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Floating Tool Bar Pill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. PEN TOOL
                ToolIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Kalem",
                    isActive = activeTool == InkToolType.PEN,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onSelectTool(InkToolType.PEN)
                        isThicknessMenuOpen = false
                    },
                    testTag = "tool_pen"
                )

                // 2. HIGHLIGHTER TOOL
                ToolIconButton(
                    icon = Icons.Default.Highlight,
                    contentDescription = "Fosforlu Kalem",
                    isActive = activeTool == InkToolType.HIGHLIGHTER,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onSelectTool(InkToolType.HIGHLIGHTER)
                        isThicknessMenuOpen = false
                    },
                    testTag = "tool_highlighter"
                )

                // 3. ERASER TOOL
                ToolIconButton(
                    icon = Icons.Default.AutoFixHigh,
                    contentDescription = "Silgi",
                    isActive = activeTool == InkToolType.ERASER,
                    activeColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        onSelectTool(InkToolType.ERASER)
                        isColorPaletteOpen = false
                        isThicknessMenuOpen = false
                    },
                    testTag = "tool_eraser"
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 4. ACTIVE COLOR DOT (opens palette)
                if (activeTool != InkToolType.ERASER) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                            .clickable {
                                isColorPaletteOpen = !isColorPaletteOpen
                                isThicknessMenuOpen = false
                            }
                            .testTag("tool_color_picker_trigger")
                    )

                    // 5. THICKNESS INDICATOR (opens thickness menu)
                    IconButton(
                        onClick = {
                            isThicknessMenuOpen = !isThicknessMenuOpen
                            isColorPaletteOpen = false
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tool_thickness_trigger")
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(when (selectedThickness) {
                                    PenThickness.THIN -> 2.dp
                                    PenThickness.MEDIUM -> 4.dp
                                    PenThickness.THICK -> 6.dp
                                })
                                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(2.dp))
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // 6. UNDO
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("tool_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Geri Al",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 7. REDO
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("tool_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Yinele",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 8. PALM REJECTION / STYLUS ONLY TOGGLE
                IconButton(
                    onClick = onToggleStylusOnly,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (stylusOnlyMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                        )
                        .testTag("tool_stylus_mode_toggle")
                ) {
                    Icon(
                        imageVector = if (stylusOnlyMode) Icons.Default.Create else Icons.Default.Gesture,
                        contentDescription = if (stylusOnlyMode) "Yalnızca Kalem (Avuç İçi Reddi Açık)" else "Parmakla Çizim Açık",
                        tint = if (stylusOnlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 9. CLOSE PEN MODE
                IconButton(
                    onClick = onClosePenMode,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("tool_close_pen_mode")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kalem Modunu Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = if (isActive) activeColor.copy(alpha = 0.18f) else Color.Transparent,
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
