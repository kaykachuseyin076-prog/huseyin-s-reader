package com.example.ui.reader

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BookRepository
import com.example.data.local.entity.InkStrokeEntity
import com.example.ui.reader.ink.InkAction
import com.example.ui.reader.ink.InkToolType
import com.example.ui.reader.ink.PenThickness
import com.example.ui.reader.ink.RenderableStroke
import com.example.util.EpubHelper
import com.example.util.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BookRepository(application)
    private val context = application.applicationContext

    val geminiRepository = com.example.data.gemini.GeminiRepository(application)
    val discussionDao = repository.getDiscussionDao()
    val notebookRepository = com.example.data.repository.NotebookRepository(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var highlightsJob: Job? = null
    private var inkStrokesJob: Job? = null
    private var notebooksJob: Job? = null
    private var allInkJob: Job? = null

    // Undo/Redo stacks for handwriting in current page/session
    private val inkUndoStack = mutableListOf<InkAction>()
    private val inkRedoStack = mutableListOf<InkAction>()

    companion object {
        private const val TAG = "ReaderViewModel"
    }

    fun loadBook(bookId: String, targetPage: Int? = null) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState(isLoading = true)
            try {
                val book = repository.getBookById(bookId)
                if (book == null) {
                    _uiState.value = ReaderUiState(
                        isLoading = false,
                        errorMessage = "Kitap bulunamadı."
                    )
                    return@launch
                }

                val format = book.format
                val totalPages = book.totalPages.coerceAtLeast(1)
                val initialPage = targetPage?.coerceIn(0, totalPages - 1) ?: book.currentPage.coerceAtLeast(0)

                _uiState.value = ReaderUiState(
                    book = book,
                    format = format,
                    currentPage = initialPage,
                    totalPages = totalPages,
                    isLoading = true,
                    areControlsVisible = false
                )

                // Observe highlights and notes for this book from Room
                observeHighlights(book.id)

                // Observe linked notebooks for this book
                observeBookNotebooks(book.id)

                // Observe all ink pages for this book
                observeAllBookInkPages(book.id)

                // Observe ink handwriting strokes for current page
                observeInkStrokesForCurrentPage(book.id, format, initialPage)

                // Load Table of Contents asynchronously
                loadTableOfContents(book.uriString, format, totalPages)

                // Render current location
                renderCurrentLocation(book.uriString, format, initialPage, totalPages)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading book $bookId", e)
                _uiState.value = ReaderUiState(
                    isLoading = false,
                    errorMessage = "Kitap yüklenirken hata oluştu: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun observeBookNotebooks(bookId: String) {
        notebooksJob?.cancel()
        notebooksJob = viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.getNotebooksForBook(bookId).collect { list ->
                _uiState.value = _uiState.value.copy(bookNotebooks = list)
            }
        }
    }

    private fun observeAllBookInkPages(bookId: String) {
        allInkJob?.cancel()
        allInkJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getAllInkStrokesForBook(bookId).collect { strokes ->
                val distinctPages = strokes.map { it.pageOrChapter }.distinct().sorted()
                _uiState.value = _uiState.value.copy(inkPages = distinctPages)
            }
        }
    }

    private fun observeHighlights(bookId: String) {
        highlightsJob?.cancel()
        highlightsJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getHighlightsForBook(bookId).collect { list ->
                _uiState.value = _uiState.value.copy(highlights = list)
            }
        }
    }

    private fun observeInkStrokesForCurrentPage(bookId: String, format: String, page: Int) {
        inkStrokesJob?.cancel()
        inkUndoStack.clear()
        inkRedoStack.clear()
        _uiState.value = _uiState.value.copy(
            canUndoStroke = false,
            canRedoStroke = false,
            currentPageStrokes = emptyList()
        )

        inkStrokesJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getInkStrokesForPage(bookId, format, page).collect { entities ->
                val renderables = entities.map { RenderableStroke.fromEntity(it) }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(currentPageStrokes = renderables)
                }
            }
        }
    }

    private fun loadTableOfContents(uriString: String, format: String, totalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val toc = if (format.equals("PDF", ignoreCase = true)) {
                    PdfHelper.extractTableOfContents(context, uri, totalPages)
                } else {
                    EpubHelper.extractTableOfContents(context, uri)
                }
                _uiState.value = _uiState.value.copy(tocItems = toc)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load TOC", e)
            }
        }
    }

    private suspend fun renderCurrentLocation(
        uriString: String,
        format: String,
        pageIndex: Int,
        totalPages: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            if (format.equals("PDF", ignoreCase = true)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPage = pageIndex,
                    errorMessage = null
                )
            } else {
                val chapter = EpubHelper.readChapter(context, uri, pageIndex)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    epubChapter = chapter,
                    currentPage = pageIndex,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering location $pageIndex", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "İçerik yüklenemedi: ${e.localizedMessage}"
            )
        }
    }

    private fun prefetchAdjacentPages(uri: Uri, currentPage: Int, totalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentPage + 1 < totalPages) {
                PdfHelper.renderPage(context, uri, currentPage + 1, 1200)
            }
            if (currentPage - 1 >= 0) {
                PdfHelper.renderPage(context, uri, currentPage - 1, 1200)
            }
        }
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(
            areControlsVisible = !_uiState.value.areControlsVisible
        )
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(areControlsVisible = visible)
    }

    fun updatePdfPage(pageIndex: Int) {
        val currentBook = _uiState.value.book ?: return
        val total = _uiState.value.totalPages
        val safePage = pageIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
        if (_uiState.value.currentPage == safePage) return

        _uiState.value = _uiState.value.copy(currentPage = safePage)
        viewModelScope.launch {
            repository.updateReadingProgress(currentBook.id, safePage, total)
            observeInkStrokesForCurrentPage(currentBook.id, _uiState.value.format, safePage)
        }
    }

    fun goToPage(pageIndex: Int) {
        val currentBook = _uiState.value.book ?: return
        val total = _uiState.value.totalPages
        val safePage = pageIndex.coerceIn(0, (total - 1).coerceAtLeast(0))

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            renderCurrentLocation(
                currentBook.uriString,
                _uiState.value.format,
                safePage,
                total
            )

            // Save reading position in Room database
            repository.updateReadingProgress(currentBook.id, safePage, total)

            // Switch ink page strokes
            observeInkStrokesForCurrentPage(currentBook.id, _uiState.value.format, safePage)
        }
    }

    fun nextPage() {
        val next = _uiState.value.currentPage + 1
        if (next < _uiState.value.totalPages) {
            goToPage(next)
        }
    }

    fun prevPage() {
        val prev = _uiState.value.currentPage - 1
        if (prev >= 0) {
            goToPage(prev)
        }
    }

    // Modal and sheet visibility handlers
    fun showToc(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isTocVisible = show)
    }

    fun showSettings(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isSettingsVisible = show)
    }

    fun showSearch(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            isSearchVisible = show,
            searchQuery = if (show) _uiState.value.searchQuery else "",
            searchResults = if (show) _uiState.value.searchResults else emptyList()
        )
    }

    fun showGoToPage(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isGoToPageVisible = show)
    }

    fun showHighlightsOverview(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isHighlightsOverviewVisible = show)
    }

    // ==========================================
    // GEMINI AI INTEGRATION (AŞAMA 5)
    // ==========================================

    fun openAiTranslate(text: String, highlightId: String? = null) {
        _uiState.value = _uiState.value.copy(
            isAiTranslateVisible = true,
            isAiHelpVisible = false,
            isAiDiscussionVisible = false,
            aiActiveSelectedText = text,
            aiActiveHighlightId = highlightId
        )
    }

    fun closeAiTranslate() {
        _uiState.value = _uiState.value.copy(isAiTranslateVisible = false)
    }

    fun openAiHelp(text: String, highlightId: String? = null) {
        _uiState.value = _uiState.value.copy(
            isAiHelpVisible = true,
            isAiTranslateVisible = false,
            isAiDiscussionVisible = false,
            aiActiveSelectedText = text,
            aiActiveHighlightId = highlightId
        )
    }

    fun closeAiHelp() {
        _uiState.value = _uiState.value.copy(isAiHelpVisible = false)
    }

    fun openAiDiscussion(text: String, highlightId: String? = null) {
        _uiState.value = _uiState.value.copy(
            isAiDiscussionVisible = true,
            isAiTranslateVisible = false,
            isAiHelpVisible = false,
            aiActiveSelectedText = text,
            aiActiveHighlightId = highlightId
        )
    }

    fun closeAiDiscussion() {
        _uiState.value = _uiState.value.copy(isAiDiscussionVisible = false)
    }

    fun closeAiSheets() {
        _uiState.value = _uiState.value.copy(
            isAiTranslateVisible = false,
            isAiHelpVisible = false,
            isAiDiscussionVisible = false
        )
    }

    fun showGeminiSettings(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isGeminiSettingsVisible = show)
    }

    fun saveAiResultAsPostIt(resultText: String) {
        val book = _uiState.value.book ?: return
        val currentHighlightId = _uiState.value.aiActiveHighlightId
        val selectedText = _uiState.value.aiActiveSelectedText.ifBlank { _uiState.value.selectedTextForHighlight }

        viewModelScope.launch(Dispatchers.IO) {
            val targetHighlightId = if (!currentHighlightId.isNullOrBlank()) {
                currentHighlightId
            } else {
                val newHl = com.example.data.local.entity.HighlightEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    bookId = book.id,
                    format = _uiState.value.format,
                    chapterOrPageIndex = _uiState.value.currentPage,
                    paragraphIndex = _uiState.value.selectedParagraphIndex,
                    startOffset = _uiState.value.selectedStartOffset,
                    endOffset = _uiState.value.selectedEndOffset,
                    selectedText = selectedText.ifBlank { "Alıntı" },
                    colorId = HighlightColor.YELLOW.id
                )
                repository.addHighlight(newHl)
                newHl.id
            }

            repository.saveNote(
                highlightId = targetHighlightId,
                bookId = book.id,
                noteText = resultText
            )

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "AI yanıtı Post-it notu olarak kaydedildi."
                )
            }
        }
    }

    // Settings adjustments
    fun updateSettings(newSettings: ReaderSettings) {
        _uiState.value = _uiState.value.copy(settings = newSettings)
    }

    fun updateTheme(theme: ReaderTheme) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(theme = theme)
        )
    }

    fun updateFontFamily(fontFamily: ReaderFontFamily) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(fontFamily = fontFamily)
        )
    }

    fun increaseFontSize() {
        val current = _uiState.value.settings.fontSizeSp
        if (current < 32f) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(fontSizeSp = (current + 2f).coerceAtMost(32f))
            )
        }
    }

    fun decreaseFontSize() {
        val current = _uiState.value.settings.fontSizeSp
        if (current > 12f) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(fontSizeSp = (current - 2f).coerceAtLeast(12f))
            )
        }
    }

    fun updateLineSpacing(spacing: Float) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(lineSpacingMultiplier = spacing)
        )
    }

    fun updateHorizontalPadding(paddingDp: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(horizontalPaddingDp = paddingDp)
        )
    }

    fun toggleProgressFormat() {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(
                showProgressAsPercentage = !_uiState.value.settings.showProgressAsPercentage
            )
        )
    }

    // Search in book
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val book = _uiState.value.book ?: return@launch
            val uri = Uri.parse(book.uriString)

            val results = if (book.format.equals("EPUB", ignoreCase = true)) {
                EpubHelper.searchInEpub(context, uri, query)
            } else {
                // PDF search: real extraction across pages
                PdfHelper.searchInPdf(context, uri, query, _uiState.value.totalPages, book.title)
            }

            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    // ==========================================
    // HIGHLIGHT & POST-IT ACTIONS (AŞAMA 3 & 8)
    // ==========================================

    fun prepareHighlightSelection(
        selectedText: String,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
        fullText: String,
        normLeft: Float = 0f,
        normTop: Float = 0f,
        normRight: Float = 0f,
        normBottom: Float = 0f
    ) {
        _uiState.value = _uiState.value.copy(
            isSelectionSheetOpen = true,
            selectedTextForHighlight = selectedText,
            selectedParagraphIndex = paragraphIndex,
            selectedStartOffset = startOffset,
            selectedEndOffset = endOffset,
            fullParagraphText = fullText,
            selectedNormLeft = normLeft,
            selectedNormTop = normTop,
            selectedNormRight = normRight,
            selectedNormBottom = normBottom
        )
    }

    fun closeSelectionSheet() {
        _uiState.value = _uiState.value.copy(isSelectionSheetOpen = false)
    }

    fun createHighlight(
        selectedText: String,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
        color: HighlightColor,
        addNoteImmediately: Boolean = false,
        normLeft: Float = _uiState.value.selectedNormLeft,
        normTop: Float = _uiState.value.selectedNormTop,
        normRight: Float = _uiState.value.selectedNormRight,
        normBottom: Float = _uiState.value.selectedNormBottom
    ) {
        val book = _uiState.value.book ?: return
        val highlightId = java.util.UUID.randomUUID().toString()

        val highlight = com.example.data.local.entity.HighlightEntity(
            id = highlightId,
            bookId = book.id,
            format = _uiState.value.format,
            chapterOrPageIndex = _uiState.value.currentPage,
            paragraphIndex = paragraphIndex,
            startOffset = startOffset,
            endOffset = endOffset,
            selectedText = selectedText,
            colorId = color.id,
            normLeft = normLeft,
            normTop = normTop,
            normRight = normRight,
            normBottom = normBottom,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.addHighlight(highlight)
            if (addNoteImmediately) {
                withContext(Dispatchers.Main) {
                    openPostIt(highlight, null)
                }
            }
        }
    }

    fun openPostIt(
        highlight: com.example.data.local.entity.HighlightEntity,
        note: com.example.data.local.entity.NoteEntity?
    ) {
        _uiState.value = _uiState.value.copy(
            isPostItOpen = true,
            activePostItHighlight = highlight,
            activePostItNote = note
        )
    }

    fun openPostItFromHighlightWithNote(item: com.example.data.local.entity.HighlightWithNote) {
        openPostIt(item.highlight, item.note)
    }

    fun closePostIt() {
        _uiState.value = _uiState.value.copy(
            isPostItOpen = false,
            activePostItHighlight = null,
            activePostItNote = null
        )
    }

    fun savePostItNote(highlightId: String, noteText: String) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNote(
                highlightId = highlightId,
                bookId = book.id,
                noteText = noteText
            )
            withContext(Dispatchers.Main) {
                closePostIt()
            }
        }
    }

    fun createNotebookForCurrentPage(
        template: String = "BOOK_NOTE",
        customTitle: String? = null,
        onCreated: (com.example.data.local.entity.NotebookEntity) -> Unit
    ) {
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        val format = _uiState.value.format
        val chapterTitle = if (format.equals("EPUB", ignoreCase = true)) {
            _uiState.value.epubChapter?.title ?: "Bölüm ${page + 1}"
        } else null
        val snippet = if (format.equals("PDF", ignoreCase = true)) {
            _uiState.value.pdfPageText.take(240).ifBlank { null }
        } else null

        viewModelScope.launch(Dispatchers.IO) {
            val notebookId = notebookRepository.createBookLinkedNotebook(
                bookId = book.id,
                bookTitle = book.title,
                pageOrChapter = page,
                format = format,
                chapterTitle = chapterTitle,
                snippet = snippet,
                customTitle = customTitle,
                template = template
            )
            val created = notebookRepository.getNotebook(notebookId)
            if (created != null) {
                withContext(Dispatchers.Main) {
                    onCreated(created)
                }
            }
        }
    }

    fun createNotebookForHighlight(
        highlight: com.example.data.local.entity.HighlightEntity,
        onCreated: (com.example.data.local.entity.NotebookEntity) -> Unit
    ) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Check if one already exists for this highlight
            val existing = notebookRepository.getNotebookForHighlightSync(highlight.id)
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    onCreated(existing)
                }
                return@launch
            }

            val format = highlight.format
            val page = highlight.chapterOrPageIndex
            val locationDesc = if (format.equals("EPUB", ignoreCase = true)) "Bölüm ${page + 1}" else "Sayfa ${page + 1}"
            val notebookId = notebookRepository.createBookLinkedNotebook(
                bookId = book.id,
                bookTitle = book.title,
                pageOrChapter = page,
                format = format,
                chapterTitle = locationDesc,
                highlightId = highlight.id,
                snippet = highlight.selectedText,
                customTitle = "${book.title} — $locationDesc Vurgu Notu",
                template = "BOOK_NOTE"
            )
            val created = notebookRepository.getNotebook(notebookId)
            if (created != null) {
                withContext(Dispatchers.Main) {
                    onCreated(created)
                }
            }
        }
    }

    fun deletePostItNote(highlightId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNoteByHighlightId(highlightId)
            withContext(Dispatchers.Main) {
                closePostIt()
                _uiState.value = _uiState.value.copy(snackbarMessage = "Not silindi")
            }
        }
    }

    fun deleteHighlight(highlightId: String) {
        val toDelete = _uiState.value.highlights.firstOrNull { it.highlight.id == highlightId }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHighlight(highlightId)
            withContext(Dispatchers.Main) {
                closePostIt()
                _uiState.value = _uiState.value.copy(
                    undoDeletedHighlight = toDelete?.highlight,
                    undoDeletedNote = toDelete?.note,
                    snackbarMessage = "Vurgu silindi"
                )
            }
        }
    }

    fun undoDeleteHighlight() {
        val highlight = _uiState.value.undoDeletedHighlight ?: return
        val note = _uiState.value.undoDeletedNote
        viewModelScope.launch(Dispatchers.IO) {
            repository.addHighlight(highlight, note?.noteText)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    undoDeletedHighlight = null,
                    undoDeletedNote = null,
                    snackbarMessage = "Vurgu geri yüklendi"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    // ==========================================
    // INK / PEN TOOL ACTIONS (AŞAMA 4)
    // ==========================================

    fun togglePenMode(active: Boolean? = null) {
        val newActive = active ?: !_uiState.value.isPenModeActive
        _uiState.value = _uiState.value.copy(
            isPenModeActive = newActive,
            // When opening pen mode, hide standard reading controls to give maximum canvas space
            areControlsVisible = if (newActive) false else _uiState.value.areControlsVisible
        )
    }

    fun selectInkTool(tool: InkToolType) {
        val newColor = if (tool == InkToolType.HIGHLIGHTER) {
            // Pick default highlighter color (Yellow)
            androidx.compose.ui.graphics.Color(0xFFFFD54F)
        } else if (_uiState.value.activeInkTool == InkToolType.HIGHLIGHTER) {
            // Switching back to pen, restore pen dark color
            androidx.compose.ui.graphics.Color(0xFF1E1E1E)
        } else {
            _uiState.value.selectedInkColor
        }

        _uiState.value = _uiState.value.copy(
            activeInkTool = tool,
            selectedInkColor = newColor
        )
    }

    fun selectInkColor(color: androidx.compose.ui.graphics.Color) {
        _uiState.value = _uiState.value.copy(selectedInkColor = color)
    }

    fun selectInkThickness(thickness: PenThickness) {
        _uiState.value = _uiState.value.copy(selectedInkThickness = thickness)
    }

    fun toggleStylusOnlyMode() {
        _uiState.value = _uiState.value.copy(stylusOnlyMode = !_uiState.value.stylusOnlyMode)
    }

    fun addInkStroke(stroke: RenderableStroke) {
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        val format = _uiState.value.format

        inkUndoStack.add(InkAction.Add(stroke))
        inkRedoStack.clear()
        _uiState.value = _uiState.value.copy(
            canUndoStroke = inkUndoStack.isNotEmpty(),
            canRedoStroke = false
        )

        val entity = InkStrokeEntity(
            id = stroke.id,
            bookId = book.id,
            format = format,
            pageOrChapter = page,
            toolType = stroke.toolType.name,
            color = stroke.color.value.toLong(),
            strokeWidth = stroke.strokeWidthDp,
            alpha = stroke.alpha,
            pointsData = InkStrokeEntity.serializePoints(stroke.points)
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertInkStroke(entity)
        }
    }

    fun eraseInkStrokes(strokeIds: List<String>) {
        if (strokeIds.isEmpty()) return
        val book = _uiState.value.book ?: return
        val erasedStrokes = _uiState.value.currentPageStrokes.filter { it.id in strokeIds }
        if (erasedStrokes.isEmpty()) return

        inkUndoStack.add(InkAction.Erase(erasedStrokes))
        inkRedoStack.clear()
        _uiState.value = _uiState.value.copy(
            canUndoStroke = inkUndoStack.isNotEmpty(),
            canRedoStroke = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInkStrokesByIds(strokeIds)
        }
    }

    fun undoInk() {
        if (inkUndoStack.isEmpty()) return
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        val format = _uiState.value.format

        val action = inkUndoStack.removeAt(inkUndoStack.lastIndex)
        inkRedoStack.add(action)
        _uiState.value = _uiState.value.copy(
            canUndoStroke = inkUndoStack.isNotEmpty(),
            canRedoStroke = inkRedoStack.isNotEmpty()
        )

        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is InkAction.Add -> {
                    repository.deleteInkStrokeById(action.stroke.id)
                }
                is InkAction.Erase -> {
                    val entities = action.strokes.map { s ->
                        InkStrokeEntity(
                            id = s.id,
                            bookId = book.id,
                            format = format,
                            pageOrChapter = page,
                            toolType = s.toolType.name,
                            color = s.color.value.toLong(),
                            strokeWidth = s.strokeWidthDp,
                            alpha = s.alpha,
                            pointsData = InkStrokeEntity.serializePoints(s.points)
                        )
                    }
                    repository.insertInkStrokes(entities)
                }
            }
        }
    }

    fun redoInk() {
        if (inkRedoStack.isEmpty()) return
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        val format = _uiState.value.format

        val action = inkRedoStack.removeAt(inkRedoStack.lastIndex)
        inkUndoStack.add(action)
        _uiState.value = _uiState.value.copy(
            canUndoStroke = inkUndoStack.isNotEmpty(),
            canRedoStroke = inkRedoStack.isNotEmpty()
        )

        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is InkAction.Add -> {
                    val s = action.stroke
                    val entity = InkStrokeEntity(
                        id = s.id,
                        bookId = book.id,
                        format = format,
                        pageOrChapter = page,
                        toolType = s.toolType.name,
                        color = s.color.value.toLong(),
                        strokeWidth = s.strokeWidthDp,
                        alpha = s.alpha,
                        pointsData = InkStrokeEntity.serializePoints(s.points)
                    )
                    repository.insertInkStroke(entity)
                }
                is InkAction.Erase -> {
                    repository.deleteInkStrokesByIds(action.strokes.map { it.id })
                }
            }
        }
    }
}
