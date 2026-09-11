package com.example.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HighlightWithNote
import com.example.util.EpubHelper
import kotlin.math.roundToInt

/**
 * Paragraph text renderer for EPUB that supports:
 * 1. Desktop-style character-level text selection (single letter, word, sub-word, phrase)
 * 2. Visual selection handles and real-time highlighted bounding rectangles
 * 3. Underline rendering under highlighted text, with large circular dots (🟠━━━━🟠) when notes exist.
 *
 * Automatically scales and recalculates layout on font size changes or device rotation.
 */
@Composable
fun HighlightedParagraphText(
    paragraph: EpubHelper.ChapterParagraph,
    highlights: List<HighlightWithNote>,
    settings: ReaderSettings,
    onHighlightClicked: (HighlightWithNote) -> Unit,
    onTextSelected: (selectedText: String, startOffset: Int, endOffset: Int) -> Unit,
    onTapBlank: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = paragraph.text
    val theme = settings.theme
    val fontFamily = settings.fontFamily.composeFontFamily
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Character-level active selection state
    var isSelecting by remember { mutableStateOf(false) }
    var selStartOffset by remember { mutableIntStateOf(0) }
    var selEndOffset by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val handleSize = 18.dp
    val handleRadiusPx = with(density) { (handleSize / 2).toPx() }

    val fontSize = if (paragraph.isHeading) {
        (settings.fontSizeSp + 3).sp
    } else {
        settings.fontSize
    }

    val lineHeight = if (paragraph.isHeading) {
        (settings.fontSizeSp * 1.3f).sp
    } else {
        settings.lineHeight
    }

    val fontWeight = if (paragraph.isHeading) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val layout = textLayoutResult ?: return@drawBehind
                if (text.isEmpty()) return@drawBehind

                val strokeWidth = 3.dp.toPx()
                val dotRadius = 5.5.dp.toPx()

                // 1. Draw saved underline highlights
                for (item in highlights) {
                    val hl = item.highlight
                    val color = HighlightColor.fromId(hl.colorId)
                    val startOffset = hl.startOffset.coerceIn(0, text.length)
                    val endOffset = hl.endOffset.coerceIn(0, text.length)
                    if (startOffset >= endOffset) continue

                    val startLine = layout.getLineForOffset(startOffset)
                    val endLine = layout.getLineForOffset(endOffset)

                    // Draw colored underline along each line segment
                    for (line in startLine..endLine) {
                        val lineStart = layout.getLineStart(line)
                        val lineEnd = layout.getLineEnd(line)
                        val segStart = maxOf(startOffset, lineStart)
                        val segEnd = minOf(endOffset, lineEnd)
                        if (segStart >= segEnd) continue

                        val x1 = layout.getHorizontalPosition(segStart, true)
                        val x2 = layout.getHorizontalPosition(segEnd, true)
                        val minX = minOf(x1, x2)
                        val maxX = maxOf(x1, x2)
                        val y = layout.getLineBottom(line) - 1.dp.toPx()

                        drawLine(
                            color = color.lineColor,
                            start = Offset(minX, y),
                            end = Offset(maxX, y),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw the two large dots ONLY if the highlight has an attached note!
                    // 🟠━━━━━━━━━━━━━━━━━━━━🟠
                    if (item.hasNote) {
                        val startX = layout.getHorizontalPosition(startOffset, true)
                        val startLineIdx = layout.getLineForOffset(startOffset)
                        val startY = layout.getLineBottom(startLineIdx) - 1.dp.toPx()
                        drawCircle(
                            color = color.dotColor,
                            radius = dotRadius,
                            center = Offset(startX, startY)
                        )

                        val endX = layout.getHorizontalPosition(endOffset, true)
                        val endLineIdx = layout.getLineForOffset(endOffset)
                        val endY = layout.getLineBottom(endLineIdx) - 1.dp.toPx()
                        drawCircle(
                            color = color.dotColor,
                            radius = dotRadius,
                            center = Offset(endX, endY)
                        )
                    }
                }

                // 2. Draw live desktop-style character selection background
                if (isSelecting) {
                    val actualStart = minOf(selStartOffset, selEndOffset).coerceIn(0, text.length)
                    val actualEnd = maxOf(selStartOffset, selEndOffset).coerceIn(0, text.length)

                    if (actualStart < actualEnd) {
                        val startLine = layout.getLineForOffset(actualStart)
                        val endLine = layout.getLineForOffset(actualEnd)

                        for (line in startLine..endLine) {
                            val lineStart = layout.getLineStart(line)
                            val lineEnd = layout.getLineEnd(line)
                            val segStart = maxOf(actualStart, lineStart)
                            val segEnd = minOf(actualEnd, lineEnd)
                            if (segStart >= segEnd) continue

                            val x1 = layout.getHorizontalPosition(segStart, true)
                            val x2 = layout.getHorizontalPosition(segEnd, true)
                            val minX = minOf(x1, x2)
                            val maxX = maxOf(x1, x2)
                            val topY = layout.getLineTop(line)
                            val bottomY = layout.getLineBottom(line)

                            drawRect(
                                color = Color(0x553F51B5),
                                topLeft = Offset(minX, topY),
                                size = Size(maxX - minX, bottomY - topY)
                            )
                        }
                    }
                }
            }
            .pointerInput(highlights, text) {
                // Multi-gesture detection: desktop drag selection + tap / long-press
                detectDragGestures(
                    onDragStart = { pos ->
                        val layout = textLayoutResult
                        if (layout != null && text.isNotEmpty()) {
                            val offset = layout.getOffsetForPosition(pos).coerceIn(0, text.length)
                            isSelecting = true
                            selStartOffset = offset
                            selEndOffset = offset
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val layout = textLayoutResult
                        if (layout != null && text.isNotEmpty() && isSelecting) {
                            val offset = layout.getOffsetForPosition(change.position).coerceIn(0, text.length)
                            selEndOffset = offset
                        }
                    },
                    onDragEnd = {
                        if (isSelecting) {
                            val start = minOf(selStartOffset, selEndOffset)
                            val end = maxOf(selStartOffset, selEndOffset)
                            if (start < end && end <= text.length) {
                                val selected = text.substring(start, end)
                                if (selected.isNotEmpty()) {
                                    onTextSelected(selected, start, end)
                                }
                            }
                            isSelecting = false
                        }
                    },
                    onDragCancel = {
                        isSelecting = false
                    }
                )
            }
            .pointerInput(highlights, text) {
                detectTapGestures(
                    onDoubleTap = { pos ->
                        val layout = textLayoutResult
                        if (layout != null && text.isNotEmpty()) {
                            val offset = layout.getOffsetForPosition(pos)
                            val range = findWordRange(text, offset)
                            if (range.first < range.second) {
                                val selected = text.substring(range.first, range.second)
                                if (selected.isNotBlank()) {
                                    selStartOffset = range.first
                                    selEndOffset = range.second
                                    isSelecting = true
                                    onTextSelected(selected, range.first, range.second)
                                }
                            }
                        }
                    },
                    onTap = { pos ->
                        if (isSelecting) {
                            isSelecting = false
                            return@detectTapGestures
                        }
                        val layout = textLayoutResult
                        if (layout != null && text.isNotEmpty()) {
                            val offset = layout.getOffsetForPosition(pos)
                            val hit = highlights.firstOrNull {
                                val hlStart = (it.highlight.startOffset - 2).coerceAtLeast(0)
                                val hlEnd = (it.highlight.endOffset + 2).coerceAtMost(text.length)
                                offset in hlStart..hlEnd
                            }
                            if (hit != null) {
                                onHighlightClicked(hit)
                            } else {
                                onTapBlank()
                            }
                        } else {
                            onTapBlank()
                        }
                    },
                    onLongPress = { pos ->
                        val layout = textLayoutResult
                        if (layout != null && text.isNotEmpty()) {
                            val offset = layout.getOffsetForPosition(pos)
                            // If user long-presses an existing highlight, open its Post-it
                            val hit = highlights.firstOrNull {
                                val hlStart = (it.highlight.startOffset - 2).coerceAtLeast(0)
                                val hlEnd = (it.highlight.endOffset + 2).coerceAtMost(text.length)
                                offset in hlStart..hlEnd
                            }
                            if (hit != null) {
                                onHighlightClicked(hit)
                            } else {
                                // Default long press selects single word precisely at character offset
                                val range = findWordRange(text, offset)
                                if (range.first < range.second) {
                                    val selected = text.substring(range.first, range.second)
                                    if (selected.isNotBlank()) {
                                        selStartOffset = range.first
                                        selEndOffset = range.second
                                        isSelecting = true
                                        onTextSelected(selected, range.first, range.second)
                                    }
                                } else {
                                    // Single letter fallback
                                    val charStart = offset.coerceIn(0, text.length)
                                    val charEnd = (offset + 1).coerceAtMost(text.length)
                                    if (charStart < charEnd) {
                                        val singleChar = text.substring(charStart, charEnd)
                                        selStartOffset = charStart
                                        selEndOffset = charEnd
                                        isSelecting = true
                                        onTextSelected(singleChar, charStart, charEnd)
                                    }
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            color = theme.textColor,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (paragraph.isHeading) 16.dp else 0.dp,
                    bottom = if (paragraph.isHeading) 8.dp else 14.dp
                )
                .testTag("epub_paragraph_${paragraph.index}")
        )

        // Desktop-style interactive selection drag handles
        val layout = textLayoutResult
        if (isSelecting && layout != null && text.isNotEmpty()) {
            val actualStart = minOf(selStartOffset, selEndOffset).coerceIn(0, text.length)
            val actualEnd = maxOf(selStartOffset, selEndOffset).coerceIn(0, text.length)

            if (actualStart < actualEnd) {
                val startX = layout.getHorizontalPosition(actualStart, true)
                val startLine = layout.getLineForOffset(actualStart)
                val startBottom = layout.getLineBottom(startLine)

                val endX = layout.getHorizontalPosition(actualEnd, true)
                val endLine = layout.getLineForOffset(actualEnd)
                val endBottom = layout.getLineBottom(endLine)

                // Start handle
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (startX - handleRadiusPx).roundToInt(),
                                (startBottom - 2.dp.toPx()).roundToInt()
                            )
                        }
                        .size(handleSize)
                        .background(Color(0xFF3F51B5), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    val newOffset = layout.getOffsetForPosition(change.position).coerceIn(0, text.length)
                                    selStartOffset = newOffset
                                },
                                onDragEnd = {
                                    val s = minOf(selStartOffset, selEndOffset)
                                    val e = maxOf(selStartOffset, selEndOffset)
                                    if (s < e) {
                                        val sel = text.substring(s, e)
                                        if (sel.isNotEmpty()) onTextSelected(sel, s, e)
                                    }
                                }
                            )
                        }
                )

                // End handle
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (endX - handleRadiusPx).roundToInt(),
                                (endBottom - 2.dp.toPx()).roundToInt()
                            )
                        }
                        .size(handleSize)
                        .background(Color(0xFF3F51B5), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    val newOffset = layout.getOffsetForPosition(change.position).coerceIn(0, text.length)
                                    selEndOffset = newOffset
                                },
                                onDragEnd = {
                                    val s = minOf(selStartOffset, selEndOffset)
                                    val e = maxOf(selStartOffset, selEndOffset)
                                    if (s < e) {
                                        val sel = text.substring(s, e)
                                        if (sel.isNotEmpty()) onTextSelected(sel, s, e)
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

/**
 * Finds precise single word boundary around character offset.
 */
fun findWordRange(fullText: String, offset: Int): Pair<Int, Int> {
    if (fullText.isEmpty()) return Pair(0, 0)
    val safe = offset.coerceIn(0, fullText.length - 1)
    if (fullText[safe].isWhitespace()) {
        return findSentenceRange(fullText, safe)
    }
    val delimiters = ".,;:!?\"'«»()[]{}—–\n\r\t"
    var start = safe
    while (start > 0 && !fullText[start - 1].isWhitespace() && fullText[start - 1] !in delimiters) {
        start--
    }
    var end = safe
    while (end < fullText.length && !fullText[end].isWhitespace() && fullText[end] !in delimiters) {
        end++
    }
    return if (start < end) Pair(start, end) else Pair(safe, (safe + 1).coerceAtMost(fullText.length))
}

/**
 * Finds sentence boundary around character offset without trailing/leading whitespace.
 */
fun findSentenceRange(fullText: String, offset: Int): Pair<Int, Int> {
    if (fullText.isEmpty()) return Pair(0, 0)
    val safeOffset = offset.coerceIn(0, fullText.length - 1)

    // Search backwards for previous sentence delimiter
    var start = safeOffset
    while (start > 0) {
        val ch = fullText[start - 1]
        if (ch == '.' || ch == '!' || ch == '?' || ch == '\n' || ch == '…') {
            break
        }
        start--
    }
    // Skip leading whitespace and quotes
    while (start < fullText.length && (fullText[start].isWhitespace() || fullText[start] in "\"'«»“”")) {
        start++
    }

    // Search forward for next sentence delimiter
    var end = safeOffset
    while (end < fullText.length) {
        val ch = fullText[end]
        if (ch == '.' || ch == '!' || ch == '?' || ch == '\n' || ch == '…') {
            end++ // include delimiter
            break
        }
        end++
    }
    // Strip trailing whitespace
    while (end > start && fullText[end - 1].isWhitespace()) {
        end--
    }

    val finalStart = start.coerceIn(0, fullText.length)
    val finalEnd = end.coerceIn(finalStart, fullText.length)
    return Pair(finalStart, finalEnd)
}

