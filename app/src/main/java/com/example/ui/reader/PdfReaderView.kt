package com.example.ui.reader

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.reader.ink.InkCanvasOverlay
import com.example.ui.reader.ink.InkToolType
import com.example.ui.reader.ink.PenThickness
import com.example.ui.reader.ink.RenderableStroke
import com.example.util.PdfSelectionResult
import com.example.util.PdfTextExtractor
import com.example.util.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun PdfReaderView(
    bitmap: Bitmap?, // Ignored, kept for compat
    currentPage: Int,
    totalPages: Int = 1,
    highlights: List<com.example.data.local.entity.HighlightWithNote>,
    theme: ReaderTheme,
    onToggleControls: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onHighlightClicked: (com.example.data.local.entity.HighlightWithNote) -> Unit,
    isPenModeActive: Boolean = false,
    activeInkTool: InkToolType = InkToolType.PEN,
    selectedInkColor: Color = Color(0xFF1E1E1E),
    selectedInkThickness: PenThickness = PenThickness.MEDIUM,
    stylusOnlyMode: Boolean = true,
    inkStrokes: List<RenderableStroke> = emptyList(),
    onInkStrokeCompleted: (RenderableStroke) -> Unit = {},
    onInkStrokesErased: (List<String>) -> Unit = {},
    pageText: String = "", // Ignored
    bookUri: Uri? = null,
    bookTitle: String? = null,
    onSelectText: (selectedText: String, pageIndex: Int, startOffset: Int, endOffset: Int, fullText: String) -> Unit = { _, _, _, _, _ -> },
    onSelectPdfText: ((selectedText: String, pageIndex: Int, lineIndex: Int, startOffset: Int, endOffset: Int, fullText: String, normLeft: Float, normTop: Float, normRight: Float, normBottom: Float) -> Unit)? = null,
    onPageChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (bookUri == null) return

    DisposableEffect(bookUri) {
        onDispose {
            PdfHelper.closeSession()
        }
    }

    var isPaginatedMode by rememberSaveable { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages }
    )
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    )

    LaunchedEffect(isPaginatedMode) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    LaunchedEffect(currentPage, isPaginatedMode) {
        if (isPaginatedMode) {
            if (pagerState.currentPage != currentPage && !pagerState.isScrollInProgress) {
                pagerState.scrollToPage(currentPage)
            }
        } else {
            if (listState.firstVisibleItemIndex != currentPage && !listState.isScrollInProgress) {
                listState.scrollToItem(currentPage)
            }
        }
    }

    LaunchedEffect(pagerState, isPaginatedMode) {
        if (isPaginatedMode) {
            snapshotFlow { pagerState.currentPage }.collect { index ->
                if (index != currentPage) {
                    onPageChanged(index)
                }
            }
        }
    }

    LaunchedEffect(listState, isPaginatedMode) {
        if (!isPaginatedMode) {
            snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
                if (index != currentPage) {
                    onPageChanged(index)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .pointerInput(scale) {
                if (scale > 1f) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
            }
            .transformable(transformableState)
    ) {
        if (isPaginatedMode) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = scale <= 1.05f && !isPenModeActive,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) { pageIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState(), enabled = scale <= 1.05f && !isPenModeActive)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PdfPageItem(
                        pageIndex = pageIndex,
                        bookUri = bookUri,
                        bookTitle = bookTitle,
                        theme = theme,
                        highlights = highlights.filter { it.highlight.chapterOrPageIndex == pageIndex },
                        inkStrokes = inkStrokes.filter { it.pageOrChapter == pageIndex },
                        isPenModeActive = isPenModeActive,
                        activeInkTool = activeInkTool,
                        selectedInkColor = selectedInkColor,
                        selectedInkThickness = selectedInkThickness,
                        stylusOnlyMode = stylusOnlyMode,
                        onInkStrokeCompleted = { onInkStrokeCompleted(it.copy(pageOrChapter = pageIndex)) },
                        onInkStrokesErased = onInkStrokesErased,
                        onSelectText = onSelectText,
                        onSelectPdfText = onSelectPdfText,
                        onHighlightClicked = onHighlightClicked,
                        onToggleControls = onToggleControls
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                userScrollEnabled = scale <= 1.05f && !isPenModeActive,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(totalPages) { pageIndex ->
                    PdfPageItem(
                        pageIndex = pageIndex,
                        bookUri = bookUri,
                        bookTitle = bookTitle,
                        theme = theme,
                        highlights = highlights.filter { it.highlight.chapterOrPageIndex == pageIndex },
                        inkStrokes = inkStrokes.filter { it.pageOrChapter == pageIndex },
                        isPenModeActive = isPenModeActive,
                        activeInkTool = activeInkTool,
                        selectedInkColor = selectedInkColor,
                        selectedInkThickness = selectedInkThickness,
                        stylusOnlyMode = stylusOnlyMode,
                        onInkStrokeCompleted = { onInkStrokeCompleted(it.copy(pageOrChapter = pageIndex)) },
                        onInkStrokesErased = onInkStrokesErased,
                        onSelectText = onSelectText,
                        onSelectPdfText = onSelectPdfText,
                        onHighlightClicked = onHighlightClicked,
                        onToggleControls = onToggleControls
                    )
                }
            }
        }

        // View Mode Toggle (Pagination vs Continuous)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .clickable { isPaginatedMode = !isPaginatedMode }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPaginatedMode) Icons.Default.AutoStories else Icons.Default.MenuBook,
                    contentDescription = "Görünüm Değiştir",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPaginatedMode) "Sayfa Sayfa" else "Sürekli",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun PdfPageItem(
    pageIndex: Int,
    bookUri: Uri,
    bookTitle: String?,
    theme: ReaderTheme,
    highlights: List<com.example.data.local.entity.HighlightWithNote>,
    inkStrokes: List<RenderableStroke>,
    isPenModeActive: Boolean,
    activeInkTool: InkToolType,
    selectedInkColor: Color,
    selectedInkThickness: PenThickness,
    stylusOnlyMode: Boolean,
    onInkStrokeCompleted: (RenderableStroke) -> Unit,
    onInkStrokesErased: (List<String>) -> Unit,
    onSelectText: (selectedText: String, pageIndex: Int, startOffset: Int, endOffset: Int, fullText: String) -> Unit,
    onSelectPdfText: ((selectedText: String, pageIndex: Int, lineIndex: Int, startOffset: Int, endOffset: Int, fullText: String, normLeft: Float, normTop: Float, normRight: Float, normBottom: Float) -> Unit)?,
    onHighlightClicked: (com.example.data.local.entity.HighlightWithNote) -> Unit,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageTiles by remember { mutableStateOf<List<Bitmap>?>(null) }
    var singleBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageAspectRatio by remember { mutableFloatStateOf(1f / 1.414f) }
    var pageLayout by remember { mutableStateOf<com.example.util.PdfPageTextLayout?>(null) }
    
    var isSelecting by remember { mutableStateOf(false) }
    var selectionStartPoint by remember { mutableStateOf<Offset?>(null) }
    var selectionEndPoint by remember { mutableStateOf<Offset?>(null) }
    var activePdfSelection by remember { mutableStateOf<PdfSelectionResult?>(null) }

    val handleSize = 18.dp
    val handleRadiusPx = with(density) { (handleSize / 2).toPx() }

    // Dynamic width scaling based on device screen: ~1000px max for phones, higher for tablets, capped at 1600px to prevent OOM
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val targetWidth = screenWidthPx.toInt().coerceIn(800, 1600)

    DisposableEffect(bookUri, pageIndex) {
        onDispose {
            previewBitmap = null
            pageTiles = null
            singleBitmap = null
            pageLayout = null
        }
    }

    LaunchedEffect(bookUri, pageIndex, bookTitle) {
        withContext(Dispatchers.IO) {
            val dims = PdfHelper.getPageDimensions(context, bookUri, pageIndex)
            if (dims != null && dims.second > 0) {
                withContext(Dispatchers.Main) {
                    pageAspectRatio = dims.first.toFloat() / dims.second.toFloat()
                }
            }

            val prev = PdfHelper.renderPagePreview(context, bookUri, pageIndex)
            withContext(Dispatchers.Main) {
                previewBitmap = prev
            }

            val tiles = PdfHelper.renderPageTiles(context, bookUri, pageIndex, targetWidth, numTiles = 2)
            val bmp = if (tiles == null || tiles.isEmpty()) {
                PdfHelper.renderPage(context, bookUri, pageIndex, targetWidth)
            } else null

            val layout = PdfTextExtractor.getPageLayout(context, bookUri, pageIndex, bookTitle)
            withContext(Dispatchers.Main) {
                pageTiles = tiles
                singleBitmap = bmp
                pageLayout = layout
            }
        }
    }

    val highlightRects = remember(highlights, pageLayout) {
        highlights.associateWith { item ->
            val hl = item.highlight
            if (hl.normRight > 0f) {
                RectF(hl.normLeft, hl.normTop, hl.normRight, hl.normBottom)
            } else {
                pageLayout?.getBoundingRect(hl.paragraphIndex, hl.startOffset, hl.endOffset) ?: RectF()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(
                elevation = if (theme == ReaderTheme.DARK) 2.dp else 5.dp,
                shape = RoundedCornerShape(2.dp)
            )
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White)
            .pointerInput(pageTiles, singleBitmap, previewBitmap, isPenModeActive, pageLayout) {
                if (!isPenModeActive && pageLayout != null) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { startPos ->
                            val normX = (startPos.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val normY = (startPos.y / size.height.toFloat()).coerceIn(0f, 1f)
                            val initial = pageLayout?.findTextAt(normX, normY)
                            if (initial != null && initial.selectedText.isNotBlank()) {
                                isSelecting = true
                                selectionStartPoint = startPos
                                selectionEndPoint = startPos
                                activePdfSelection = initial
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (isSelecting) {
                                selectionEndPoint = change.position
                                val sPos = selectionStartPoint ?: change.position
                                val startNormX = (sPos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val startNormY = (sPos.y / size.height.toFloat()).coerceIn(0f, 1f)
                                val endNormX = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val endNormY = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)

                                val rangeResult = pageLayout?.findTextRange(startNormX, startNormY, endNormX, endNormY)
                                if (rangeResult != null) {
                                    activePdfSelection = rangeResult
                                }
                            }
                        },
                        onDragEnd = {
                            val current = activePdfSelection
                            if (isSelecting && current != null && current.selectedText.isNotBlank()) {
                                if (onSelectPdfText != null) {
                                    onSelectPdfText(
                                        current.selectedText,
                                        pageIndex,
                                        current.lineIndex,
                                        current.startOffset,
                                        current.endOffset,
                                        current.fullLineText,
                                        current.normLeft,
                                        current.normTop,
                                        current.normRight,
                                        current.normBottom
                                    )
                                } else {
                                    onSelectText(
                                        current.selectedText,
                                        pageIndex,
                                        current.startOffset,
                                        current.endOffset,
                                        current.fullLineText
                                    )
                                }
                            }
                            isSelecting = false
                        },
                        onDragCancel = {
                            isSelecting = false
                        }
                    )
                }
            }
            .pointerInput(pageTiles, singleBitmap, previewBitmap, isPenModeActive, pageLayout, highlights) {
                if (!isPenModeActive) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (isSelecting) {
                                isSelecting = false
                                activePdfSelection = null
                                return@detectTapGestures
                            }

                            val normX = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val normY = (tapOffset.y / size.height.toFloat()).coerceIn(0f, 1f)

                            val hit = highlights.firstOrNull { item ->
                                val rect = highlightRects[item] ?: RectF()
                                normX >= (rect.left - 0.05f) && normX <= (rect.right + 0.05f) &&
                                        normY >= (rect.top - 0.03f) && normY <= (rect.bottom + 0.04f)
                            }

                            if (hit != null) {
                                onHighlightClicked(hit)
                            } else {
                                onToggleControls()
                            }
                        }
                    )
                }
            }
    ) {
        val activeTiles = pageTiles?.filter { !it.isRecycled }
        val hasValidTiles = !activeTiles.isNullOrEmpty() && activeTiles.size == pageTiles?.size
        val hasValidSingle = singleBitmap != null && !singleBitmap!!.isRecycled
        val hasValidPreview = previewBitmap != null && !previewBitmap!!.isRecycled
        val hasRendered = hasValidTiles || hasValidSingle || hasValidPreview

        if (hasValidTiles) {
            Column(modifier = Modifier.fillMaxWidth()) {
                for ((tIdx, tile) in activeTiles!!.withIndex()) {
                    Image(
                        bitmap = tile.asImageBitmap(),
                        contentDescription = "PDF Sayfası ${pageIndex + 1} - Parça ${tIdx + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (hasValidSingle) {
            Image(
                bitmap = singleBitmap!!.asImageBitmap(),
                contentDescription = "PDF Sayfası ${pageIndex + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (hasValidPreview) {
            Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = "PDF Sayfası ${pageIndex + 1} Önizleme",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(pageAspectRatio),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                )
            }
        }

        if (hasRendered) {

            if (highlights.isNotEmpty()) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    val pageHeight = size.height
                    val pageWidth = size.width

                    for (item in highlights) {
                        val hl = item.highlight
                        val color = HighlightColor.fromId(hl.colorId)

                        val rect = highlightRects[item] ?: RectF()

                        val startX = (rect.left * pageWidth).coerceIn(0f, pageWidth)
                        val endX = (rect.right * pageWidth).coerceIn(0f, pageWidth)
                        val topY = (rect.top * pageHeight).coerceIn(0f, pageHeight)
                        val bottomY = (rect.bottom * pageHeight).coerceIn(0f, pageHeight)

                        if (endX > startX && bottomY >= topY) {
                            val rectHeight = (bottomY - topY).coerceAtLeast(14.dp.toPx())

                            drawRect(
                                color = color.chipColor.copy(alpha = 0.28f),
                                topLeft = Offset(startX, topY),
                                size = Size(endX - startX, rectHeight)
                            )

                            val underlineY = topY + rectHeight
                            drawLine(
                                color = color.lineColor,
                                start = Offset(startX, underlineY),
                                end = Offset(endX, underlineY),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            if (item.hasNote) {
                                drawCircle(
                                    color = color.dotColor,
                                    radius = 5.5.dp.toPx(),
                                    center = Offset(startX, underlineY)
                                )
                                drawCircle(
                                    color = color.dotColor,
                                    radius = 5.5.dp.toPx(),
                                    center = Offset(endX, underlineY)
                                )
                            }
                        }
                    }
                }
            }

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

            val activeSel = activePdfSelection
            if (activeSel != null && activeSel.selectedText.isNotBlank()) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    val pageWidth = size.width
                    val pageHeight = size.height
                    val sX = (activeSel.normLeft * pageWidth).coerceIn(0f, pageWidth)
                    val eX = (activeSel.normRight * pageWidth).coerceIn(0f, pageWidth)
                    val tY = (activeSel.normTop * pageHeight).coerceIn(0f, pageHeight)
                    val bY = (activeSel.normBottom * pageHeight).coerceIn(0f, pageHeight)
                    val rectH = (bY - tY).coerceAtLeast(14.dp.toPx())

                    if (eX > sX) {
                        drawRect(
                            color = Color(0x443F51B5),
                            topLeft = Offset(sX, tY),
                            size = Size(eX - sX, rectH)
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier.matchParentSize()
                ) {
                    val currentWidthPx = constraints.maxWidth.toFloat()
                    val currentHeightPx = constraints.maxHeight.toFloat()
                    val sXPx = currentWidthPx * activeSel.normLeft
                    val eXPx = currentWidthPx * activeSel.normRight
                    val tYPx = currentHeightPx * activeSel.normTop
                    val bYPx = currentHeightPx * activeSel.normBottom

                    val pad24Px = with(density) { 24.dp.toPx() }
                    val pad10Px = with(density) { 10.dp.toPx() }
                    val pad56Px = with(density) { 56.dp.toPx() }
                    val pad8Px = with(density) { 8.dp.toPx() }
                    val pad120Px = with(density) { 120.dp.toPx() }
                    val pad130Px = with(density) { 130.dp.toPx() }

                    // Start Handle (with 48dp accessible touch target)
                    var startDragX by remember { mutableFloatStateOf(0f) }
                    var startDragY by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (sXPx - pad24Px).roundToInt(),
                                    (bYPx - pad10Px).roundToInt()
                                )
                            }
                            .size(48.dp)
                            .pointerInput(activeSel) {
                                detectDragGestures(
                                    onDragStart = {
                                        startDragX = sXPx
                                        startDragY = bYPx
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        startDragX += dragAmount.x
                                        startDragY += dragAmount.y
                                        val newNormX = (startDragX / currentWidthPx).coerceIn(0f, 1f)
                                        val newNormY = (startDragY / currentHeightPx).coerceIn(0f, 1f)
                                        val updated = pageLayout?.findTextRange(
                                            newNormX,
                                            newNormY,
                                            activeSel.normRight,
                                            activeSel.normBottom
                                        )
                                        if (updated != null) activePdfSelection = updated
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(handleSize)
                                .background(Color(0xFF3F51B5), CircleShape)
                        )
                    }

                    // End Handle (with 48dp accessible touch target)
                    var endDragX by remember { mutableFloatStateOf(0f) }
                    var endDragY by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (eXPx - pad24Px).roundToInt(),
                                    (bYPx - pad10Px).roundToInt()
                                )
                            }
                            .size(48.dp)
                            .pointerInput(activeSel) {
                                detectDragGestures(
                                    onDragStart = {
                                        endDragX = eXPx
                                        endDragY = bYPx
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        endDragX += dragAmount.x
                                        endDragY += dragAmount.y
                                        val newNormX = (endDragX / currentWidthPx).coerceIn(0f, 1f)
                                        val newNormY = (endDragY / currentHeightPx).coerceIn(0f, 1f)
                                        val updated = pageLayout?.findTextRange(
                                            activeSel.normLeft,
                                            activeSel.normTop,
                                            newNormX,
                                            newNormY
                                        )
                                        if (updated != null) activePdfSelection = updated
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(handleSize)
                                .background(Color(0xFF3F51B5), CircleShape)
                        )
                    }

                    // Floating Action Toolbar right above selection
                    val toolbarTopPx = (tYPx - pad56Px).coerceAtLeast(pad8Px)
                    val toolbarCenterXPx = ((sXPx + eXPx) / 2f).coerceIn(
                        pad130Px,
                        (currentWidthPx - pad130Px).coerceAtLeast(pad130Px)
                    )

                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (toolbarCenterXPx - pad120Px).roundToInt(),
                                    toolbarTopPx.roundToInt()
                                )
                            }
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color chips for 1-tap instant highlighting
                            val colors = listOf(
                                HighlightColor.YELLOW,
                                HighlightColor.GREEN,
                                HighlightColor.BLUE,
                                HighlightColor.PINK,
                                HighlightColor.ORANGE
                            )
                            colors.forEach { hlColor ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(hlColor.chipColor)
                                        .clickable {
                                            val current = activePdfSelection
                                            if (current != null) {
                                                onSelectPdfText?.invoke(
                                                    current.selectedText,
                                                    pageIndex,
                                                    current.lineIndex,
                                                    current.startOffset,
                                                    current.endOffset,
                                                    current.fullLineText,
                                                    current.normLeft,
                                                    current.normTop,
                                                    current.normRight,
                                                    current.normBottom
                                                )
                                            }
                                            activePdfSelection = null
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Post-It Note button
                            IconButton(
                                onClick = {
                                    val current = activePdfSelection
                                    if (current != null) {
                                        onSelectPdfText?.invoke(
                                            current.selectedText,
                                            pageIndex,
                                            current.lineIndex,
                                            current.startOffset,
                                            current.endOffset,
                                            current.fullLineText,
                                            current.normLeft,
                                            current.normTop,
                                            current.normRight,
                                            current.normBottom
                                        )
                                    }
                                    activePdfSelection = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StickyNote2,
                                    contentDescription = "Not Ekle",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Copy button
                            IconButton(
                                onClick = {
                                    val textToCopy = activePdfSelection?.selectedText ?: ""
                                    if (textToCopy.isNotBlank()) {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("PDF Text", textToCopy)
                                        clipboard?.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Metin kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    activePdfSelection = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Dismiss button
                            IconButton(
                                onClick = { activePdfSelection = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Highlights chips
            if (highlights.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vurgular:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (hl in highlights) {
                        val color = HighlightColor.fromId(hl.highlight.colorId)
                        Surface(
                            color = color.postItBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, color.postItBorder),
                            modifier = Modifier
                                .clickable { onHighlightClicked(hl) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hl.hasNote) {
                                    Icon(
                                        imageVector = Icons.Default.StickyNote2,
                                        contentDescription = null,
                                        tint = color.lineColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = hl.highlight.selectedText.take(18) + if (hl.highlight.selectedText.length > 18) "…" else "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = color.lineColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
