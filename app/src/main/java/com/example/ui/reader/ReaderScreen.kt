package com.example.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tune
import com.example.ui.reader.ink.PenToolbar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    targetPage: Int? = null,
    onOpenNotebook: ((notebookId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Keep screen awake while reading
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(bookId, targetPage) {
        viewModel.loadBook(bookId, targetPage)
    }

    val theme = state.settings.theme
    val isPdf = state.format.equals("PDF", ignoreCase = true)
    val entityName = if (isPdf) "Sayfa" else "Bölüm"

    val tocSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .testTag("reader_screen_root")
    ) {
        // Main Book Content Layer
        when {
            state.errorMessage != null -> {
                // Error Display
                ReaderErrorView(
                    message = state.errorMessage ?: "Bilinmeyen bir hata oluştu.",
                    onRetry = { viewModel.loadBook(bookId) },
                    onBack = onBack,
                    theme = theme
                )
            }
            isPdf -> {
                PdfReaderView(
                    bitmap = state.pdfPageBitmap,
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    highlights = state.highlights,
                    theme = theme,
                    onToggleControls = { viewModel.toggleControls() },
                    onPrevPage = { viewModel.prevPage() },
                    onNextPage = { viewModel.nextPage() },
                    onHighlightClicked = { viewModel.openPostItFromHighlightWithNote(it) },
                    isPenModeActive = state.isPenModeActive,
                    activeInkTool = state.activeInkTool,
                    selectedInkColor = state.selectedInkColor,
                    selectedInkThickness = state.selectedInkThickness,
                    stylusOnlyMode = state.stylusOnlyMode,
                    inkStrokes = state.currentPageStrokes,
                    onInkStrokeCompleted = { viewModel.addInkStroke(it) },
                    onInkStrokesErased = { viewModel.eraseInkStrokes(it) },
                    pageText = state.pdfPageText,
                    bookUri = state.book?.uriString?.let { android.net.Uri.parse(it) },
                    bookTitle = state.book?.title,
                    onPageChanged = { viewModel.updatePdfPage(it) },
                    onSelectText = { selectedText, pIdx, startOffset, endOffset, fullText ->
                        viewModel.prepareHighlightSelection(
                            selectedText = selectedText,
                            paragraphIndex = pIdx,
                            startOffset = startOffset,
                            endOffset = endOffset,
                            fullText = fullText
                        )
                    },
                    onSelectPdfText = { selectedText, pageIndex, lineIndex, startOffset, endOffset, fullText, normLeft, normTop, normRight, normBottom ->
                        viewModel.prepareHighlightSelection(
                            selectedText = selectedText,
                            paragraphIndex = lineIndex,
                            startOffset = startOffset,
                            endOffset = endOffset,
                            fullText = fullText,
                            normLeft = normLeft,
                            normTop = normTop,
                            normRight = normRight,
                            normBottom = normBottom
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                EpubReaderView(
                    chapter = state.epubChapter,
                    chapterIndex = state.currentPage,
                    totalChapters = state.totalPages,
                    settings = state.settings,
                    highlights = state.highlights,
                    onToggleControls = { viewModel.toggleControls() },
                    onPrevChapter = { viewModel.prevPage() },
                    onNextChapter = { viewModel.nextPage() },
                    onHighlightClicked = { viewModel.openPostItFromHighlightWithNote(it) },
                    onSelectText = { selected, pIdx, start, end, fullText ->
                        viewModel.prepareHighlightSelection(selected, pIdx, start, end, fullText)
                    },
                    isPenModeActive = state.isPenModeActive,
                    activeInkTool = state.activeInkTool,
                    selectedInkColor = state.selectedInkColor,
                    selectedInkThickness = state.selectedInkThickness,
                    stylusOnlyMode = state.stylusOnlyMode,
                    inkStrokes = state.currentPageStrokes,
                    onInkStrokeCompleted = { viewModel.addInkStroke(it) },
                    onInkStrokesErased = { viewModel.eraseInkStrokes(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Discreet Loading Overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.backgroundColor.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = theme.surfaceColor,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = theme.accentColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Yükleniyor…",
                            color = theme.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Minimal Unobtrusive Page Indicator (Shown when controls are hidden)
        if (!state.areControlsVisible && !state.isLoading && state.errorMessage == null) {
            val navInsets = WindowInsets.navigationBars.asPaddingValues()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navInsets.calculateBottomPadding() + 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.backgroundColor.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                val minimalLabel = if (state.settings.showProgressAsPercentage) {
                    "%${state.progressPercentage}"
                } else {
                    "${state.currentPage + 1} / ${state.totalPages}"
                }
                Text(
                    text = minimalLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = theme.secondaryTextColor.copy(alpha = 0.8f)
                )
            }
        }

        // Animated Reading Controls Overlay (Top & Bottom Bars)
        AnimatedVisibility(
            visible = state.areControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopControls(
                title = state.book?.title ?: "Kitap",
                theme = theme,
                onBack = onBack,
                onOpenToc = { viewModel.showToc(true) },
                onOpenSearch = { viewModel.showSearch(true) },
                onOpenHighlights = { viewModel.showHighlightsOverview(true) },
                onAddNote = {
                    viewModel.createNotebookForCurrentPage { notebook ->
                        onOpenNotebook?.invoke(notebook.id)
                    }
                },
                onOpenSettings = { viewModel.showSettings(true) },
                isPenModeActive = state.isPenModeActive,
                onTogglePenMode = { viewModel.togglePenMode() }
            )
        }

        // Floating Quick Pen Button (when controls are hidden and pen mode not yet active)
        if (!state.areControlsVisible && !state.isPenModeActive && !state.isLoading && state.errorMessage == null) {
            val statusInsets = WindowInsets.statusBars.asPaddingValues()
            Surface(
                shape = CircleShape,
                color = theme.surfaceColor.copy(alpha = 0.88f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusInsets.calculateTopPadding() + 10.dp, end = 16.dp)
                    .testTag("floating_quick_pen_button")
            ) {
                IconButton(
                    onClick = { viewModel.togglePenMode(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Kalem Modunu Aç",
                        tint = theme.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Active Floating Pen Toolbar (Aşama 4)
        AnimatedVisibility(
            visible = state.isPenModeActive,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
        ) {
            PenToolbar(
                activeTool = state.activeInkTool,
                selectedColor = state.selectedInkColor,
                selectedThickness = state.selectedInkThickness,
                canUndo = state.canUndoStroke,
                canRedo = state.canRedoStroke,
                stylusOnlyMode = state.stylusOnlyMode,
                onSelectTool = { viewModel.selectInkTool(it) },
                onSelectColor = { viewModel.selectInkColor(it) },
                onSelectThickness = { viewModel.selectInkThickness(it) },
                onUndo = { viewModel.undoInk() },
                onRedo = { viewModel.redoInk() },
                onToggleStylusOnly = { viewModel.toggleStylusOnlyMode() },
                onClosePenMode = { viewModel.togglePenMode(false) }
            )
        }

        AnimatedVisibility(
            visible = state.areControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderBottomControls(
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                progressPercentage = state.progressPercentage,
                entityName = entityName,
                theme = theme,
                onPrevPage = { viewModel.prevPage() },
                onNextPage = { viewModel.nextPage() },
                onSliderChange = { newPage -> viewModel.goToPage(newPage) },
                onOpenGoToPage = { viewModel.showGoToPage(true) },
                onToggleProgressFormat = { viewModel.toggleProgressFormat() }
            )
        }

        // Modal Sheets and Dialogs
        if (state.isTocVisible) {
            TableOfContentsSheet(
                sheetState = tocSheetState,
                tocItems = state.tocItems,
                currentIndex = state.currentPage,
                format = state.format,
                onSelectItem = { targetIndex ->
                    viewModel.goToPage(targetIndex)
                    viewModel.showToc(false)
                },
                onDismiss = { viewModel.showToc(false) }
            )
        }

        if (state.isSettingsVisible) {
            ReaderSettingsSheet(
                sheetState = settingsSheetState,
                settings = state.settings,
                format = state.format,
                onThemeChange = { viewModel.updateTheme(it) },
                onIncreaseFontSize = { viewModel.increaseFontSize() },
                onDecreaseFontSize = { viewModel.decreaseFontSize() },
                onLineSpacingChange = { viewModel.updateLineSpacing(it) },
                onHorizontalPaddingChange = { viewModel.updateHorizontalPadding(it) },
                onFontFamilyChange = { viewModel.updateFontFamily(it) },
                onDismiss = { viewModel.showSettings(false) }
            )
        }

        if (state.isSearchVisible) {
            InBookSearchSheet(
                sheetState = searchSheetState,
                query = state.searchQuery,
                searchResults = state.searchResults,
                isSearching = state.isSearching,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSelectResult = { targetIndex ->
                    viewModel.goToPage(targetIndex)
                    viewModel.showSearch(false)
                },
                onDismiss = { viewModel.showSearch(false) }
            )
        }

        if (state.isGoToPageVisible) {
            GoToPageDialog(
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                format = state.format,
                onConfirm = { targetIndex ->
                    viewModel.goToPage(targetIndex)
                },
                onDismiss = { viewModel.showGoToPage(false) }
            )
        }

        // ==========================================
        // HIGHLIGHT & POST-IT MODALS (AŞAMA 3)
        // ==========================================

        // 1. Post-it Note Modal / Dialog
        if (state.isPostItOpen && state.activePostItHighlight != null) {
            PostItNoteDialog(
                highlight = state.activePostItHighlight!!,
                existingNote = state.activePostItNote,
                onSaveNote = { noteText ->
                    viewModel.savePostItNote(state.activePostItHighlight!!.id, noteText)
                },
                onDeleteNote = {
                    viewModel.deletePostItNote(state.activePostItHighlight!!.id)
                },
                onDeleteHighlight = {
                    viewModel.deleteHighlight(state.activePostItHighlight!!.id)
                },
                onTranslate = {
                    viewModel.openAiTranslate(state.activePostItHighlight!!.selectedText, state.activePostItHighlight!!.id)
                },
                onAiHelp = {
                    viewModel.openAiHelp(state.activePostItHighlight!!.selectedText, state.activePostItHighlight!!.id)
                },
                onDiscuss = {
                    viewModel.openAiDiscussion(state.activePostItHighlight!!.selectedText, state.activePostItHighlight!!.id)
                },
                onOpenDetailedNote = {
                    viewModel.createNotebookForHighlight(state.activePostItHighlight!!) { notebook ->
                        onOpenNotebook?.invoke(notebook.id)
                    }
                },
                onDismiss = { viewModel.closePostIt() }
            )
        }

        // 2. Text Selection & Highlight Creator Sheet
        if (state.isSelectionSheetOpen) {
            val selectionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            HighlightSelectionSheet(
                sheetState = selectionSheetState,
                initialSelectedText = state.selectedTextForHighlight,
                paragraphText = state.fullParagraphText.ifEmpty { state.selectedTextForHighlight },
                initialStartOffset = state.selectedStartOffset,
                initialEndOffset = state.selectedEndOffset,
                onConfirmHighlight = { selectedText, startOffset, endOffset, color, addNoteImmediately ->
                    viewModel.createHighlight(
                        selectedText = selectedText,
                        paragraphIndex = state.selectedParagraphIndex,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        color = color,
                        addNoteImmediately = addNoteImmediately,
                        normLeft = state.selectedNormLeft,
                        normTop = state.selectedNormTop,
                        normRight = state.selectedNormRight,
                        normBottom = state.selectedNormBottom
                    )
                    viewModel.closeSelectionSheet()
                },
                onTranslate = { text ->
                    viewModel.openAiTranslate(text)
                },
                onAiHelp = { text ->
                    viewModel.openAiHelp(text)
                },
                onDiscuss = { text ->
                    viewModel.openAiDiscussion(text)
                },
                onDetailedNote = { selectedText, startOffset, endOffset, color ->
                    val highlight = com.example.data.local.entity.HighlightEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        bookId = state.book?.id ?: "",
                        format = state.format,
                        chapterOrPageIndex = state.currentPage,
                        paragraphIndex = state.selectedParagraphIndex,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        selectedText = selectedText,
                        colorId = color.id,
                        normLeft = state.selectedNormLeft,
                        normTop = state.selectedNormTop,
                        normRight = state.selectedNormRight,
                        normBottom = state.selectedNormBottom,
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.createHighlight(
                        selectedText = selectedText,
                        paragraphIndex = state.selectedParagraphIndex,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        color = color,
                        addNoteImmediately = false,
                        normLeft = state.selectedNormLeft,
                        normTop = state.selectedNormTop,
                        normRight = state.selectedNormRight,
                        normBottom = state.selectedNormBottom
                    )
                    viewModel.createNotebookForHighlight(highlight) { notebook ->
                        onOpenNotebook?.invoke(notebook.id)
                    }
                },
                onDismiss = { viewModel.closeSelectionSheet() }
            )
        }

        // 3. Book Highlights & Notes Overview Sheet
        if (state.isHighlightsOverviewVisible) {
            val highlightsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            BookHighlightsSheet(
                sheetState = highlightsSheetState,
                highlights = state.highlights,
                format = state.format,
                bookTitle = state.book?.title ?: "Kitap",
                currentPage = state.currentPage,
                notebooks = state.bookNotebooks,
                inkPages = state.inkPages,
                onCreateNotebookForCurrentPage = {
                    viewModel.createNotebookForCurrentPage {
                        onOpenNotebook?.invoke(it.id)
                    }
                },
                onSelectNotebook = {
                    onOpenNotebook?.invoke(it.id)
                },
                onSelectHighlight = { item ->
                    viewModel.showHighlightsOverview(false)
                    viewModel.goToPage(item.highlight.chapterOrPageIndex)
                    viewModel.openPostItFromHighlightWithNote(item)
                },
                onOpenDetailedNoteForHighlight = { item ->
                    viewModel.createNotebookForHighlight(item.highlight) {
                        onOpenNotebook?.invoke(it.id)
                    }
                },
                onNavigateToPage = { page ->
                    viewModel.showHighlightsOverview(false)
                    viewModel.goToPage(page)
                },
                onDeleteHighlight = { hlId ->
                    viewModel.deleteHighlight(hlId)
                },
                onDismiss = { viewModel.showHighlightsOverview(false) }
            )
        }

        // 4. Floating Feedback Snackbar with "Geri Al" (Undo)
        if (state.snackbarMessage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(12.dp))
                    .testTag("reader_snackbar"),
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.snackbarMessage ?: "",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        modifier = Modifier.weight(1f)
                    )

                    if (state.undoDeletedHighlight != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.undoDeleteHighlight() },
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Text(
                                text = "Geri Al",
                                color = MaterialTheme.colorScheme.inversePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            LaunchedEffect(state.snackbarMessage) {
                kotlinx.coroutines.delay(4000)
                viewModel.clearSnackbar()
            }
        }

        // Gemini AI Settings Sheet
        if (state.isGeminiSettingsVisible) {
            val geminiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            com.example.ui.reader.ai.GeminiSettingsSheet(
                sheetState = geminiSheetState,
                onDismiss = { viewModel.showGeminiSettings(false) }
            )
        }

        // 5. Gemini AI Sheets (Çevir, AI Yardım, Tartış)
        if (state.isAiTranslateVisible) {
            val translateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            com.example.ui.reader.ai.TranslateSheet(
                sheetState = translateSheetState,
                selectedText = state.aiSelectedText,
                geminiRepository = viewModel.geminiRepository,
                onSaveAsPostItNote = { translation ->
                    viewModel.saveAiResultAsPostIt(translation)
                },
                onOpenSettings = {
                    viewModel.showGeminiSettings(true)
                },
                onDismiss = {
                    viewModel.closeAiSheets()
                }
            )
        }

        if (state.isAiHelpVisible) {
            val aiHelpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            com.example.ui.reader.ai.AiHelpSheet(
                sheetState = aiHelpSheetState,
                selectedText = state.aiSelectedText,
                bookTitle = state.book?.title,
                geminiRepository = viewModel.geminiRepository,
                onSaveAsPostItNote = { noteText ->
                    viewModel.saveAiResultAsPostIt(noteText)
                },
                onOpenSettings = {
                    viewModel.showGeminiSettings(true)
                },
                onDismiss = {
                    viewModel.closeAiSheets()
                }
            )
        }

        if (state.isAiDiscussionVisible) {
            val discussionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            com.example.ui.reader.ai.DiscussionChatSheet(
                sheetState = discussionSheetState,
                selectedText = state.aiSelectedText,
                highlightId = state.aiHighlightId,
                bookId = bookId,
                discussionDao = viewModel.discussionDao,
                geminiRepository = viewModel.geminiRepository,
                onOpenSettings = {
                    viewModel.showGeminiSettings(true)
                },
                onDismiss = {
                    viewModel.closeAiSheets()
                }
            )
        }
    }
}

@Composable
fun ReaderTopControls(
    title: String,
    theme: ReaderTheme,
    onBack: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHighlights: () -> Unit,
    onOpenSettings: () -> Unit,
    isPenModeActive: Boolean = false,
    onTogglePenMode: () -> Unit = {},
    onAddNote: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusBarInsets = WindowInsets.statusBars.asPaddingValues()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp),
        color = theme.surfaceColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarInsets.calculateTopPadding(), bottom = 6.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("reader_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = theme.textColor
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = theme.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )

            // Table of Contents
            IconButton(
                onClick = onOpenToc,
                modifier = Modifier.testTag("reader_toc_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "İçindekiler",
                    tint = theme.textColor
                )
            }

            // In-Book Search
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.testTag("reader_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ara",
                    tint = theme.textColor
                )
            }

            // Highlights and Post-it Notes
            IconButton(
                onClick = onOpenHighlights,
                modifier = Modifier.testTag("reader_highlights_button")
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.StickyNote2,
                    contentDescription = "Vurgular ve Notlar",
                    tint = theme.textColor
                )
            }

            // Add Note for Current Book Page (Aşama 7)
            if (onAddNote != null) {
                IconButton(
                    onClick = onAddNote,
                    modifier = Modifier.testTag("reader_add_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Not Ekle",
                        tint = theme.textColor
                    )
                }
            }

            // Tablet Stylus / Handwriting Mode (Aşama 4)
            IconButton(
                onClick = onTogglePenMode,
                modifier = Modifier
                    .testTag("reader_pen_mode_button")
                    .then(
                        if (isPenModeActive) Modifier.background(theme.accentColor.copy(alpha = 0.2f), CircleShape)
                        else Modifier
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Kalem Modu",
                    tint = if (isPenModeActive) theme.accentColor else theme.textColor
                )
            }

            // Settings
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("reader_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Okuma Ayarları",
                    tint = theme.textColor
                )
            }
        }
    }
}

@Composable
fun ReaderBottomControls(
    currentPage: Int,
    totalPages: Int,
    progressPercentage: Int,
    entityName: String,
    theme: ReaderTheme,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onSliderChange: (Int) -> Unit,
    onOpenGoToPage: () -> Unit,
    onToggleProgressFormat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
    var sliderValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp),
        color = theme.surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = navBarInsets.calculateBottomPadding())
        ) {
            // Slider Row with Prev / Next Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevPage,
                    enabled = currentPage > 0,
                    modifier = Modifier.testTag("reader_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Önceki $entityName",
                        tint = if (currentPage > 0) theme.textColor else theme.secondaryTextColor.copy(alpha = 0.35f)
                    )
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        onSliderChange(sliderValue.toInt())
                    },
                    valueRange = 0f..(totalPages - 1).coerceAtLeast(0).toFloat(),
                    steps = (totalPages - 2).coerceAtLeast(0),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("reader_page_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accentColor,
                        activeTrackColor = theme.accentColor,
                        inactiveTrackColor = theme.borderColor
                    )
                )

                IconButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.testTag("reader_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Sonraki $entityName",
                        tint = if (currentPage < totalPages - 1) theme.textColor else theme.secondaryTextColor.copy(alpha = 0.35f)
                    )
                }
            }

            // Bottom Info Bar: Go to Page / Progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Page / Chapter Info (Opens Go to Page Dialog)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenGoToPage() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        tint = theme.accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$entityName ${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = theme.textColor
                    )
                }

                // Progress Percentage (clickable to toggle formatting)
                Text(
                    text = "%$progressPercentage tamamlandı",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = theme.secondaryTextColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleProgressFormat() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ReaderErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    theme: ReaderTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.surfaceColor,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kitap Açılamadı",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.textColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.secondaryTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(onClick = onBack) {
                        Text("Kütüphaneye Dön")
                    }

                    Button(onClick = onRetry) {
                        Text("Yeniden Dene")
                    }
                }
            }
        }
    }
}
