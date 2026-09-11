package com.example.ui.notebook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    notebookId: String,
    onBack: () -> Unit,
    onNavigateToBook: ((bookId: String, pageOrChapter: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: NotebookViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = isSystemInDarkTheme()

    val sourceBook = state.notebook
    val sourceLocationText = remember(sourceBook) {
        if (sourceBook == null) ""
        else if (sourceBook.pdfPage != null) "Sayfa ${sourceBook.pdfPage + 1}"
        else if (!sourceBook.chapterTitle.isNullOrBlank()) sourceBook.chapterTitle
        else if (sourceBook.chapterIndex != null) "Bölüm ${sourceBook.chapterIndex + 1}"
        else "Kitap Notu"
    }
    val targetPage = remember(sourceBook) {
        sourceBook?.pdfPage ?: sourceBook?.chapterIndex ?: 0
    }

    var newTextBoxCoords by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    LaunchedEffect(notebookId) {
        viewModel.loadNotebook(notebookId)
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("notebook_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NotebookToolbar(
                title = state.notebook?.title ?: "Not Defteri",
                currentPageIndex = state.currentPageIndex,
                totalPages = maxOf(1, state.pages.size),
                currentTool = state.activeTool,
                selectedColor = state.selectedColor,
                selectedThickness = state.selectedThickness,
                selectedShapeType = state.selectedShapeType,
                stylusOnlyMode = state.stylusOnlyMode,
                zoomScale = state.zoomScale,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onToolSelected = { viewModel.setTool(it) },
                onColorSelected = { c, l -> viewModel.setColor(c, l) },
                onThicknessSelected = { viewModel.setThickness(it) },
                onShapeTypeSelected = { viewModel.setShapeType(it) },
                onStylusOnlyToggled = { viewModel.toggleStylusOnly() },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onZoomReset = { viewModel.resetZoom() },
                onZoomLevelSelected = { viewModel.setZoomLevel(it) },
                onPrevPage = { viewModel.goToPage(state.currentPageIndex - 1) },
                onNextPage = { viewModel.goToPage(state.currentPageIndex + 1) },
                onAddPage = { viewModel.addNewPage() },
                onOpenPageManager = { viewModel.showPageManager(true) },
                onTitleClicked = { viewModel.showRenameDialog(true) },
                onChangeTemplateClicked = { viewModel.showChangeTemplateDialog(true) },
                onDeleteNotebookClicked = { viewModel.showDeleteConfirm(true) },
                onBack = onBack,
                sourceBookTitle = sourceBook?.bookTitle,
                sourceLocationText = sourceLocationText,
                onNavigateToSource = if (sourceBook?.bookId != null && onNavigateToBook != null) {
                    { onNavigateToBook(sourceBook.bookId, targetPage) }
                } else null
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDark) Color(0xFF141619) else Color(0xFFE9ECF2))
        ) {
            NotebookCanvas(
                template = state.currentTemplate,
                tool = state.activeTool,
                selectedColor = state.selectedColor,
                selectedColorLong = state.selectedColorLong,
                selectedThickness = state.selectedThickness,
                selectedShapeType = state.selectedShapeType,
                stylusOnlyMode = state.stylusOnlyMode,
                isDarkMode = isDark,
                zoomScale = state.zoomScale,
                panOffset = state.panOffset,
                onZoomAndPanChanged = { s, p -> viewModel.updateZoomAndPan(s, p) },
                strokes = state.strokes,
                shapes = state.shapes,
                textBoxes = state.textBoxes,
                onStrokeCompleted = { viewModel.onStrokeCompleted(it) },
                onStrokesErased = { viewModel.onStrokesErased(it) },
                onStrokesMoved = { oldMap, moved -> viewModel.onStrokesMoved(oldMap, moved) },
                onStrokesCopied = { viewModel.onStrokesCopied(it) },
                onShapeCompleted = { viewModel.onShapeCompleted(it) },
                onTextBoxClicked = { viewModel.openTextBoxEditor(it) },
                onAddTextBoxRequested = { x, y ->
                    newTextBoxCoords = Pair(x, y)
                    viewModel.openTextBoxEditor(null)
                }
            )
        }
    }

    // ==========================================
    // DIALOGS & MODAL SHEETS
    // ==========================================

    if (state.isRenamingOpen) {
        RenameNotebookDialog(
            currentTitle = state.notebook?.title ?: "",
            onConfirm = { viewModel.renameNotebook(it) },
            onDismiss = { viewModel.showRenameDialog(false) }
        )
    }

    if (state.isChangeTemplateOpen) {
        ChangeTemplateDialog(
            currentTemplate = state.currentTemplate,
            onTemplateSelected = { viewModel.changeTemplate(it) },
            onDismiss = { viewModel.showChangeTemplateDialog(false) }
        )
    }

    if (state.isPageManagerOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PageManagerBottomSheet(
            sheetState = sheetState,
            pages = state.pages,
            currentPageIndex = state.currentPageIndex,
            onPageSelected = { viewModel.goToPage(it) },
            onAddPage = { viewModel.addNewPage() },
            onDeletePage = { viewModel.deleteCurrentPage() },
            onDismiss = { viewModel.showPageManager(false) }
        )
    }

    if (state.isDeleteConfirmOpen) {
        DeleteNotebookConfirmDialog(
            notebookTitle = state.notebook?.title ?: "Not Defteri",
            onConfirm = { viewModel.deleteNotebook(onDeleted = onBack) },
            onDismiss = { viewModel.showDeleteConfirm(false) }
        )
    }

    if (state.isTextBoxEditorOpen) {
        val existing = state.editingTextBox
        val coords = newTextBoxCoords
        TextBoxEditorDialog(
            initialText = existing?.text ?: "",
            initialFontSize = existing?.fontSizeSp ?: 16f,
            initialIsBold = existing?.isBold ?: false,
            onSave = { text, fontSize, isBold ->
                viewModel.saveTextBox(
                    text = text,
                    x = coords?.first ?: (existing?.x ?: 100f),
                    y = coords?.second ?: (existing?.y ?: 100f),
                    fontSizeSp = fontSize,
                    isBold = isBold
                )
                newTextBoxCoords = null
            },
            onDelete = if (existing != null) { { viewModel.deleteTextBox(existing) } } else null,
            onDismiss = {
                viewModel.closeTextBoxEditor()
                newTextBoxCoords = null
            }
        )
    }
}
