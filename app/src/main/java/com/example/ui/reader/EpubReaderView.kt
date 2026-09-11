package com.example.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.reader.ink.InkCanvasOverlay
import com.example.ui.reader.ink.InkToolType
import com.example.ui.reader.ink.PenThickness
import com.example.ui.reader.ink.RenderableStroke
import com.example.util.EpubHelper

@Composable
fun EpubReaderView(
    chapter: EpubHelper.ChapterContent?,
    chapterIndex: Int,
    totalChapters: Int,
    settings: ReaderSettings,
    highlights: List<com.example.data.local.entity.HighlightWithNote>,
    onToggleControls: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onHighlightClicked: (com.example.data.local.entity.HighlightWithNote) -> Unit,
    onSelectText: (selectedText: String, paragraphIndex: Int, startOffset: Int, endOffset: Int, fullText: String) -> Unit,
    isPenModeActive: Boolean = false,
    activeInkTool: InkToolType = InkToolType.PEN,
    selectedInkColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    selectedInkThickness: PenThickness = PenThickness.MEDIUM,
    stylusOnlyMode: Boolean = true,
    inkStrokes: List<RenderableStroke> = emptyList(),
    onInkStrokeCompleted: (RenderableStroke) -> Unit = {},
    onInkStrokesErased: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (chapter == null) return

    val scrollState = rememberScrollState()
    val theme = settings.theme
    val fontFamily = settings.fontFamily.composeFontFamily

    // Reset scroll to top when chapter changes
    LaunchedEffect(chapterIndex) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp) // Optimized for tablet and large screen readability
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = settings.horizontalPadding,
                    vertical = 24.dp
                )
                .testTag("epub_content_column")
        ) {
            // Chapter Title
            if (!chapter.title.isNullOrBlank()) {
                Text(
                    text = chapter.title,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (settings.fontSizeSp + 6).sp,
                        fontFamily = fontFamily
                    ),
                    color = theme.textColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(
                    color = theme.borderColor.copy(alpha = 0.6f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Paragraphs / Chapter Body wrapped with vector ink overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (chapter.paragraphs.isNotEmpty()) {
                        for (para in chapter.paragraphs) {
                            val paraHighlights = highlights.filter {
                                it.highlight.chapterOrPageIndex == chapterIndex && it.highlight.paragraphIndex == para.index
                            }

                            HighlightedParagraphText(
                                paragraph = para,
                                highlights = paraHighlights,
                                settings = settings,
                                onHighlightClicked = onHighlightClicked,
                                onTextSelected = { selected, start, end ->
                                    onSelectText(selected, para.index, start, end, para.text)
                                },
                                onTapBlank = onToggleControls
                            )
                        }
                    } else {
                        // Fallback to split text
                        val paragraphs = chapter.plainText.split(Regex("\n\n+"))
                        for ((idx, paraText) in paragraphs.withIndex()) {
                            val trimmed = paraText.trim()
                            if (trimmed.isNotBlank()) {
                                val pseudoPara = EpubHelper.ChapterParagraph(
                                    index = idx,
                                    text = trimmed,
                                    isHeading = false
                                )
                                val paraHighlights = highlights.filter {
                                    it.highlight.chapterOrPageIndex == chapterIndex && it.highlight.paragraphIndex == idx
                                }

                                HighlightedParagraphText(
                                    paragraph = pseudoPara,
                                    highlights = paraHighlights,
                                    settings = settings,
                                    onHighlightClicked = onHighlightClicked,
                                    onTextSelected = { selected, start, end ->
                                        onSelectText(selected, idx, start, end, trimmed)
                                    },
                                    onTapBlank = onToggleControls
                                )
                            }
                        }
                    }
                }

                // Vector ink handwriting overlay for EPUB chapter
                InkCanvasOverlay(
                    isPenModeActive = isPenModeActive,
                    activeTool = activeInkTool,
                    selectedColor = selectedInkColor,
                    selectedThickness = selectedInkThickness,
                    stylusOnlyMode = stylusOnlyMode,
                    strokes = inkStrokes,
                    onStrokeCompleted = onInkStrokeCompleted,
                    onStrokesErased = onInkStrokesErased,
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Chapter Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chapterIndex > 0) {
                    OutlinedButton(
                        onClick = onPrevChapter,
                        modifier = Modifier.testTag("epub_prev_chapter_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = theme.accentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Önceki Bölüm",
                            color = theme.accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (chapterIndex < totalChapters - 1) {
                    OutlinedButton(
                        onClick = onNextChapter,
                        modifier = Modifier.testTag("epub_next_chapter_button")
                    ) {
                        Text(
                            text = "Sonraki Bölüm",
                            color = theme.accentColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = theme.accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
