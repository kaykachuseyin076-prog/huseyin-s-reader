package com.example.ui.notebook

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 8 Professional Paper Templates for Digital Notebooks.
 */
enum class NotebookTemplate(
    val id: String,
    val title: String,
    val subtitle: String
) {
    BLANK(
        id = "BLANK",
        title = "Boş Beyaz Sayfa",
        subtitle = "Serbest çizim ve eskiz için temiz beyaz kağıt"
    ),
    LINED(
        id = "LINED",
        title = "Çizgili",
        subtitle = "Düzenli el yazısı ve not alma için standart yatay çizgiler"
    ),
    GRID(
        id = "GRID",
        title = "Kareli",
        subtitle = "Matematik, fen ve diyagramlar için kareli zemin"
    ),
    DOTTED(
        id = "DOTTED",
        title = "Noktalı",
        subtitle = "Bullet journal, çizim ve planlama için noktalı zemin"
    ),
    CORNELL(
        id = "CORNELL",
        title = "Cornell Notu",
        subtitle = "Dersler için anahtar kelime, not ve özet bölümleri"
    ),
    LECTURE(
        id = "LECTURE",
        title = "Ders Notu",
        subtitle = "Ders adı, konu ve tarih başlıklı akademik şablon"
    ),
    BOOK_NOTE(
        id = "BOOK_NOTE",
        title = "Kitap Okuma Notu",
        subtitle = "Alıntılar, kişisel düşünceler ve ana fikirler için şablon"
    ),
    SUMMARY(
        id = "SUMMARY",
        title = "Özet Sayfası",
        subtitle = "Hedef, temel maddeler ve eylem adımları için özet şablonu"
    );

    companion object {
        fun fromId(id: String): NotebookTemplate {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: BLANK
        }
    }
}

/**
 * High-performance vector rendering of notebook paper templates in virtual coordinates (0..width, 0..height).
 * Scales infinitely with zero blur or degradation during zoom.
 */
fun drawNotebookTemplateBackground(
    drawScope: DrawScope,
    template: NotebookTemplate,
    width: Float,
    height: Float,
    isDarkMode: Boolean = false
) {
    with(drawScope) {
        // Base paper background
        val paperColor = if (isDarkMode) Color(0xFF1E2024) else Color(0xFFFCFCFD)
        drawRect(color = paperColor, size = size)

        val lineColor = if (isDarkMode) Color(0xFF333842) else Color(0xFFE2E6EE)
        val marginColor = if (isDarkMode) Color(0xFF5C3838) else Color(0xFFF09595)
        val accentColor = if (isDarkMode) Color(0xFF3F4B5E) else Color(0xFFC7D7EC)
        val textColor = if (isDarkMode) Color(0xFF8E9BAE) else Color(0xFF717D96)

        when (template) {
            NotebookTemplate.BLANK -> {
                // Clean blank page with subtle outer guideline
                drawRect(
                    color = lineColor.copy(alpha = 0.5f),
                    topLeft = Offset(20f, 20f),
                    size = androidx.compose.ui.geometry.Size(width - 40f, height - 40f),
                    style = Stroke(width = 1f)
                )
            }

            NotebookTemplate.LINED -> {
                val lineSpacing = 42f
                val startY = 120f
                val marginX = 140f

                // Left vertical margin line
                drawLine(
                    color = marginColor,
                    start = Offset(marginX, 30f),
                    end = Offset(marginX, height - 30f),
                    strokeWidth = 1.5f
                )

                // Top header line
                drawLine(
                    color = lineColor,
                    start = Offset(40f, startY),
                    end = Offset(width - 40f, startY),
                    strokeWidth = 2f
                )

                // Horizontal ruled lines
                var y = startY + lineSpacing
                while (y < height - 50f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(40f, y),
                        end = Offset(width - 40f, y),
                        strokeWidth = 1f
                    )
                    y += lineSpacing
                }
            }

            NotebookTemplate.GRID -> {
                val gridSpacing = 32f
                val padding = 30f

                // Vertical grid lines
                var x = padding
                while (x <= width - padding) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, padding),
                        end = Offset(x, height - padding),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }

                // Horizontal grid lines
                var y = padding
                while (y <= height - padding) {
                    drawLine(
                        color = lineColor,
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }
            }

            NotebookTemplate.DOTTED -> {
                val dotSpacing = 32f
                val padding = 32f
                val dotRadius = 1.2f

                var y = padding
                while (y <= height - padding) {
                    var x = padding
                    while (x <= width - padding) {
                        drawCircle(
                            color = textColor.copy(alpha = 0.45f),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        x += dotSpacing
                    }
                    y += dotSpacing
                }
            }

            NotebookTemplate.CORNELL -> {
                val headerHeight = 110f
                val summaryHeight = 220f
                val cueColumnWidth = width * 0.30f
                val lineSpacing = 38f

                // Top Header separator
                drawLine(
                    color = accentColor,
                    start = Offset(30f, headerHeight),
                    end = Offset(width - 30f, headerHeight),
                    strokeWidth = 2f
                )

                // Left Cue Column separator
                drawLine(
                    color = accentColor,
                    start = Offset(cueColumnWidth, headerHeight),
                    end = Offset(cueColumnWidth, height - summaryHeight),
                    strokeWidth = 2f
                )

                // Bottom Summary separator
                drawLine(
                    color = accentColor,
                    start = Offset(30f, height - summaryHeight),
                    end = Offset(width - 30f, height - summaryHeight),
                    strokeWidth = 2f
                )

                // Outer border
                drawRect(
                    color = lineColor,
                    topLeft = Offset(30f, 30f),
                    size = androidx.compose.ui.geometry.Size(width - 60f, height - 60f),
                    style = Stroke(width = 1.5f)
                )

                // Ruled lines in note taking column
                var y = headerHeight + lineSpacing
                while (y < height - summaryHeight - 10f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(cueColumnWidth + 10f, y),
                        end = Offset(width - 35f, y),
                        strokeWidth = 1f
                    )
                    y += lineSpacing
                }

                // Ruled lines in summary section
                var sy = height - summaryHeight + lineSpacing
                while (sy < height - 40f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(40f, sy),
                        end = Offset(width - 40f, sy),
                        strokeWidth = 1f
                    )
                    sy += lineSpacing
                }
            }

            NotebookTemplate.LECTURE -> {
                val headerBottom = 130f
                val lineSpacing = 40f
                val leftMargin = 120f

                // Header box
                drawRect(
                    color = lineColor.copy(alpha = 0.35f),
                    topLeft = Offset(30f, 30f),
                    size = androidx.compose.ui.geometry.Size(width - 60f, headerBottom - 30f)
                )
                drawRect(
                    color = accentColor,
                    topLeft = Offset(30f, 30f),
                    size = androidx.compose.ui.geometry.Size(width - 60f, headerBottom - 30f),
                    style = Stroke(width = 1.5f)
                )

                // Vertical date/topic divider in header
                drawLine(
                    color = accentColor,
                    start = Offset(width * 0.65f, 30f),
                    end = Offset(width * 0.65f, headerBottom),
                    strokeWidth = 1.5f
                )

                // Vertical margin line
                drawLine(
                    color = marginColor,
                    start = Offset(leftMargin, headerBottom + 10f),
                    end = Offset(leftMargin, height - 30f),
                    strokeWidth = 1.5f
                )

                // Ruled lines
                var y = headerBottom + lineSpacing
                while (y < height - 40f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(30f, y),
                        end = Offset(width - 30f, y),
                        strokeWidth = 1f
                    )
                    y += lineSpacing
                }
            }

            NotebookTemplate.BOOK_NOTE -> {
                val headerBottom = 140f
                val midSplit = height * 0.55f

                // Header box for Book Title & Author
                drawRect(
                    color = lineColor.copy(alpha = 0.3f),
                    topLeft = Offset(30f, 30f),
                    size = androidx.compose.ui.geometry.Size(width - 60f, headerBottom - 30f)
                )
                drawRect(
                    color = accentColor,
                    topLeft = Offset(30f, 30f),
                    size = androidx.compose.ui.geometry.Size(width - 60f, headerBottom - 30f),
                    style = Stroke(width = 1.5f)
                )

                // Middle section line (Alıntılar vs Kişisel Düşünceler)
                drawLine(
                    color = accentColor,
                    start = Offset(30f, midSplit),
                    end = Offset(width - 30f, midSplit),
                    strokeWidth = 2f
                )

                // Outer border
                drawRect(
                    color = lineColor,
                    topLeft = Offset(30f, headerBottom + 15f),
                    size = androidx.compose.ui.geometry.Size(width - 60f, height - headerBottom - 45f),
                    style = Stroke(width = 1.5f)
                )

                // Ruled lines in top section
                var y1 = headerBottom + 45f
                while (y1 < midSplit - 15f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(40f, y1),
                        end = Offset(width - 40f, y1),
                        strokeWidth = 1f
                    )
                    y1 += 38f
                }

                // Ruled lines in bottom section
                var y2 = midSplit + 45f
                while (y2 < height - 45f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(40f, y2),
                        end = Offset(width - 40f, y2),
                        strokeWidth = 1f
                    )
                    y2 += 38f
                }
            }

            NotebookTemplate.SUMMARY -> {
                val block1Height = height * 0.28f
                val block2Height = height * 0.62f
                val padding = 30f

                // Outer border
                drawRect(
                    color = accentColor,
                    topLeft = Offset(padding, padding),
                    size = androidx.compose.ui.geometry.Size(width - 2 * padding, height - 2 * padding),
                    style = Stroke(width = 1.5f)
                )

                // Divider 1 (Hedef & Problem)
                drawLine(
                    color = accentColor,
                    start = Offset(padding, block1Height),
                    end = Offset(width - padding, block1Height),
                    strokeWidth = 1.5f
                )

                // Divider 2 (Ana Çözüm / Temel Maddeler)
                drawLine(
                    color = accentColor,
                    start = Offset(padding, block2Height),
                    end = Offset(width - padding, block2Height),
                    strokeWidth = 1.5f
                )

                // Ruled lines in all 3 blocks
                var y = padding + 45f
                while (y < height - padding - 15f) {
                    if (Math.abs(y - block1Height) > 15f && Math.abs(y - block2Height) > 15f) {
                        drawLine(
                            color = lineColor,
                            start = Offset(padding + 15f, y),
                            end = Offset(width - padding - 15f, y),
                            strokeWidth = 1f
                        )
                    }
                    y += 38f
                }
            }
        }
    }
}
