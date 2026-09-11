package com.example.ui.notebook

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.InkPoint
import com.example.data.local.entity.NotebookEntity
import com.example.data.local.entity.NotebookPageEntity
import com.example.data.local.entity.NotebookShapeEntity
import com.example.data.local.entity.NotebookStrokeEntity
import com.example.data.local.entity.NotebookTextBoxEntity
import com.example.data.repository.NotebookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.UUID

data class NotebookUiState(
    val notebook: NotebookEntity? = null,
    val pages: List<NotebookPageEntity> = emptyList(),
    val currentPageIndex: Int = 0,
    val currentPage: NotebookPageEntity? = null,
    val currentTemplate: NotebookTemplate = NotebookTemplate.BLANK,
    val strokes: List<NotebookStrokeEntity> = emptyList(),
    val shapes: List<NotebookShapeEntity> = emptyList(),
    val textBoxes: List<NotebookTextBoxEntity> = emptyList(),
    val activeTool: NotebookTool = NotebookTool.PEN,
    val selectedColor: Color = Color(0xFF1E1E1E),
    val selectedColorLong: Long = 0xFF1E1E1EL,
    val selectedThickness: NotebookPenThickness = NotebookPenThickness.MEDIUM,
    val selectedShapeType: NotebookShapeType = NotebookShapeType.LINE,
    val stylusOnlyMode: Boolean = false,
    val zoomScale: Float = 1.0f,
    val panOffset: Offset = Offset.Zero,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isRenamingOpen: Boolean = false,
    val isChangeTemplateOpen: Boolean = false,
    val isPageManagerOpen: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false,
    val editingTextBox: NotebookTextBoxEntity? = null,
    val isTextBoxEditorOpen: Boolean = false,
    val snackbarMessage: String? = null
)

class NotebookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotebookRepository(application)

    private val _uiState = MutableStateFlow(NotebookUiState())
    val uiState: StateFlow<NotebookUiState> = _uiState.asStateFlow()

    private var pageStrokesJob: Job? = null
    private var pageShapesJob: Job? = null
    private var pageTextBoxesJob: Job? = null

    // Undo / Redo stacks
    private val undoStack = ArrayDeque<NotebookUndoAction>(50)
    private val redoStack = ArrayDeque<NotebookUndoAction>(50)

    fun loadNotebook(notebookId: String) {
        viewModelScope.launch {
            repository.observeNotebook(notebookId).collect { nb ->
                _uiState.value = _uiState.value.copy(notebook = nb)
            }
        }

        viewModelScope.launch {
            repository.getPages(notebookId).collect { pages ->
                val currentIdx = _uiState.value.currentPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                val activePage = pages.getOrNull(currentIdx)
                val template = activePage?.template?.let { NotebookTemplate.fromId(it) } ?: NotebookTemplate.BLANK

                _uiState.value = _uiState.value.copy(
                    pages = pages,
                    currentPageIndex = currentIdx,
                    currentPage = activePage,
                    currentTemplate = template,
                    zoomScale = activePage?.zoomScale ?: 1.0f,
                    panOffset = Offset(activePage?.panOffsetX ?: 0f, activePage?.panOffsetY ?: 0f)
                )

                if (activePage != null) {
                    observePageData(activePage.id)
                }
            }
        }
    }

    private fun observePageData(pageId: String) {
        pageStrokesJob?.cancel()
        pageShapesJob?.cancel()
        pageTextBoxesJob?.cancel()

        pageStrokesJob = viewModelScope.launch {
            repository.getStrokes(pageId).collect { strokes ->
                _uiState.value = _uiState.value.copy(strokes = strokes)
            }
        }

        pageShapesJob = viewModelScope.launch {
            repository.getShapes(pageId).collect { shapes ->
                _uiState.value = _uiState.value.copy(shapes = shapes)
            }
        }

        pageTextBoxesJob = viewModelScope.launch {
            repository.getTextBoxes(pageId).collect { boxes ->
                _uiState.value = _uiState.value.copy(textBoxes = boxes)
            }
        }
    }

    fun setTool(tool: NotebookTool) {
        _uiState.value = _uiState.value.copy(activeTool = tool)
    }

    fun setColor(color: Color, colorLong: Long) {
        _uiState.value = _uiState.value.copy(selectedColor = color, selectedColorLong = colorLong)
    }

    fun setThickness(thickness: NotebookPenThickness) {
        _uiState.value = _uiState.value.copy(selectedThickness = thickness)
    }

    fun setShapeType(shapeType: NotebookShapeType) {
        _uiState.value = _uiState.value.copy(selectedShapeType = shapeType)
    }

    fun toggleStylusOnly() {
        val newMode = !_uiState.value.stylusOnlyMode
        _uiState.value = _uiState.value.copy(
            stylusOnlyMode = newMode,
            snackbarMessage = if (newMode) "Stylus Modu Açık (El Reddi Aktif)" else "Dokunmatik Mod Aktif"
        )
    }

    fun updateZoomAndPan(scale: Float, pan: Offset) {
        val clampedScale = scale.coerceIn(1.0f, 5.0f)
        _uiState.value = _uiState.value.copy(zoomScale = clampedScale, panOffset = pan)
    }

    fun resetZoom() {
        _uiState.value = _uiState.value.copy(zoomScale = 1.0f, panOffset = Offset.Zero)
    }

    fun setZoomLevel(level: Float) {
        _uiState.value = _uiState.value.copy(zoomScale = level.coerceIn(1.0f, 5.0f))
    }

    // ==========================================
    // STROKES & DRAWING ACTIONS
    // ==========================================

    fun onStrokeCompleted(stroke: NotebookStrokeEntity) {
        val activePage = _uiState.value.currentPage ?: return
        val currentNotebook = _uiState.value.notebook ?: return

        val fullStroke = stroke.copy(
            notebookId = currentNotebook.id,
            pageId = activePage.id
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStroke(fullStroke)
            pushUndo(NotebookUndoAction.AddStroke(fullStroke))
        }
    }

    fun onStrokesErased(erasedStrokes: List<NotebookStrokeEntity>) {
        if (erasedStrokes.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStrokes(erasedStrokes.map { it.id })
            pushUndo(NotebookUndoAction.EraseStrokes(erasedStrokes))
        }
    }

    fun onStrokesMoved(
        oldPointsMap: Map<String, List<InkPoint>>,
        movedStrokes: List<NotebookStrokeEntity>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStrokes(movedStrokes)
            pushUndo(NotebookUndoAction.MoveStrokes(oldPointsMap, movedStrokes))
        }
    }

    fun onStrokesCopied(copiedStrokes: List<NotebookStrokeEntity>) {
        val activePage = _uiState.value.currentPage ?: return
        val currentNotebook = _uiState.value.notebook ?: return

        val fullStrokes = copiedStrokes.map {
            it.copy(notebookId = currentNotebook.id, pageId = activePage.id)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStrokes(fullStrokes)
            fullStrokes.forEach { pushUndo(NotebookUndoAction.AddStroke(it)) }
        }
    }

    fun onShapeCompleted(shape: NotebookShapeEntity) {
        val activePage = _uiState.value.currentPage ?: return
        val currentNotebook = _uiState.value.notebook ?: return

        val fullShape = shape.copy(
            notebookId = currentNotebook.id,
            pageId = activePage.id
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveShape(fullShape)
            pushUndo(NotebookUndoAction.AddShape(fullShape))
        }
    }

    // ==========================================
    // TEXT BOX ACTIONS
    // ==========================================

    fun openTextBoxEditor(textBox: NotebookTextBoxEntity?) {
        _uiState.value = _uiState.value.copy(
            editingTextBox = textBox,
            isTextBoxEditorOpen = true
        )
    }

    fun closeTextBoxEditor() {
        _uiState.value = _uiState.value.copy(
            editingTextBox = null,
            isTextBoxEditorOpen = false
        )
    }

    fun saveTextBox(
        text: String,
        x: Float,
        y: Float,
        fontSizeSp: Float = 16f,
        isBold: Boolean = false,
        colorLong: Long = 0xFF1E1E1EL
    ) {
        val activePage = _uiState.value.currentPage ?: return
        val currentNotebook = _uiState.value.notebook ?: return
        val existing = _uiState.value.editingTextBox

        val box = NotebookTextBoxEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            notebookId = currentNotebook.id,
            pageId = activePage.id,
            text = text,
            x = existing?.x ?: x,
            y = existing?.y ?: y,
            width = existing?.width ?: 240f,
            height = existing?.height ?: 80f,
            fontSizeSp = fontSizeSp,
            isBold = isBold,
            color = colorLong
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTextBox(box)
            pushUndo(NotebookUndoAction.AddTextBox(box))
            closeTextBoxEditor()
        }
    }

    fun deleteTextBox(box: NotebookTextBoxEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTextBox(box.id)
            pushUndo(NotebookUndoAction.DeleteTextBox(box))
            closeTextBoxEditor()
        }
    }

    // ==========================================
    // UNDO & REDO
    // ==========================================

    private fun pushUndo(action: NotebookUndoAction) {
        if (undoStack.size >= 50) {
            undoStack.removeFirst()
        }
        undoStack.addLast(action)
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeLast()
        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is NotebookUndoAction.AddStroke -> {
                    repository.deleteStrokes(listOf(action.stroke.id))
                    redoStack.addLast(action)
                }

                is NotebookUndoAction.EraseStrokes -> {
                    repository.saveStrokes(action.strokes)
                    redoStack.addLast(action)
                }

                is NotebookUndoAction.MoveStrokes -> {
                    val reverted = action.newStrokes.mapNotNull { s ->
                        val oldPts = action.oldPoints[s.id] ?: return@mapNotNull null
                        s.copy(pointsData = NotebookStrokeEntity.serializePoints(oldPts))
                    }
                    repository.saveStrokes(reverted)
                    redoStack.addLast(action)
                }

                is NotebookUndoAction.AddShape -> {
                    repository.deleteShape(action.shape.id)
                    redoStack.addLast(action)
                }

                is NotebookUndoAction.DeleteShape -> {
                    repository.saveShape(action.shape)
                    redoStack.addLast(action)
                }

                is NotebookUndoAction.AddTextBox -> {
                    repository.deleteTextBox(action.textBox.id)
                    redoStack.addLast(action)
                }

                is NotebookUndoAction.DeleteTextBox -> {
                    repository.saveTextBox(action.textBox)
                    redoStack.addLast(action)
                }
            }
            updateUndoRedoAvailability()
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeLast()
        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is NotebookUndoAction.AddStroke -> {
                    repository.saveStroke(action.stroke)
                    undoStack.addLast(action)
                }

                is NotebookUndoAction.EraseStrokes -> {
                    repository.deleteStrokes(action.strokes.map { it.id })
                    undoStack.addLast(action)
                }

                is NotebookUndoAction.MoveStrokes -> {
                    repository.saveStrokes(action.newStrokes)
                    undoStack.addLast(action)
                }

                is NotebookUndoAction.AddShape -> {
                    repository.saveShape(action.shape)
                    undoStack.addLast(action)
                }

                is NotebookUndoAction.DeleteShape -> {
                    repository.deleteShape(action.shape.id)
                    undoStack.addLast(action)
                }

                is NotebookUndoAction.AddTextBox -> {
                    repository.saveTextBox(action.textBox)
                    undoStack.addLast(action)
                }

                is NotebookUndoAction.DeleteTextBox -> {
                    repository.deleteTextBox(action.textBox.id)
                    undoStack.addLast(action)
                }
            }
            updateUndoRedoAvailability()
        }
    }

    private fun updateUndoRedoAvailability() {
        _uiState.value = _uiState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    // ==========================================
    // PAGE MANAGEMENT
    // ==========================================

    fun goToPage(index: Int) {
        val pages = _uiState.value.pages
        if (index in pages.indices) {
            // Persist current page viewport
            val curPage = _uiState.value.currentPage
            if (curPage != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updatePageViewport(curPage.id, _uiState.value.zoomScale, _uiState.value.panOffset.x, _uiState.value.panOffset.y)
                }
            }

            val targetPage = pages[index]
            val template = NotebookTemplate.fromId(targetPage.template)
            _uiState.value = _uiState.value.copy(
                currentPageIndex = index,
                currentPage = targetPage,
                currentTemplate = template,
                zoomScale = targetPage.zoomScale,
                panOffset = Offset(targetPage.panOffsetX, targetPage.panOffsetY)
            )
            undoStack.clear()
            redoStack.clear()
            updateUndoRedoAvailability()
            observePageData(targetPage.id)
        }
    }

    fun addNewPage(template: String? = null) {
        val nb = _uiState.value.notebook ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newPage = repository.addPage(nb.id, template)
            launch(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Yeni sayfa eklendi"
                )
            }
        }
    }

    fun deleteCurrentPage() {
        val nb = _uiState.value.notebook ?: return
        val curPage = _uiState.value.currentPage ?: return
        if (_uiState.value.pages.size <= 1) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Son sayfa silinemez")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePage(nb.id, curPage.id)
            launch(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Sayfa silindi")
            }
        }
    }

    // ==========================================
    // NOTEBOOK SETTINGS & DIALOGS
    // ==========================================

    fun showRenameDialog(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isRenamingOpen = show)
    }

    fun renameNotebook(newTitle: String) {
        val nb = _uiState.value.notebook ?: return
        if (newTitle.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.renameNotebook(nb.id, newTitle.trim())
                showRenameDialog(false)
            }
        }
    }

    fun showChangeTemplateDialog(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isChangeTemplateOpen = show)
    }

    fun changeTemplate(newTemplate: NotebookTemplate) {
        val curPage = _uiState.value.currentPage ?: return
        val nb = _uiState.value.notebook ?: return

        viewModelScope.launch(Dispatchers.IO) {
            // Update current page template
            val database = com.example.data.local.AppDatabase.getInstance(getApplication())
            database.notebookDao().updatePage(curPage.copy(template = newTemplate.id))
            repository.updateNotebookTemplate(nb.id, newTemplate.id)

            _uiState.value = _uiState.value.copy(
                currentTemplate = newTemplate,
                isChangeTemplateOpen = false,
                snackbarMessage = "Şablon güncellendi: ${newTemplate.title}"
            )
        }
    }

    fun showPageManager(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isPageManagerOpen = show)
    }

    fun showDeleteConfirm(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(isDeleteConfirmOpen = show)
    }

    fun deleteNotebook(onDeleted: () -> Unit) {
        val nb = _uiState.value.notebook ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotebook(nb.id)
            launch(Dispatchers.Main) {
                showDeleteConfirm(false)
                onDeleted()
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
