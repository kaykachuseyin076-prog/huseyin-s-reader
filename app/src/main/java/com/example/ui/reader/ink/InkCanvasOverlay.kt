package com.example.ui.reader.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.example.data.local.entity.InkPoint
import java.util.UUID

/**
 * High-performance vector ink canvas overlay for tablet stylus handwriting and highlighter notes.
 *
 * Supports:
 * - Palm rejection / Stylus vs Finger separation (finger passes through when stylus-only is active)
 * - Hardware stylus pressure sensitivity
 * - Smooth cubic/quadratic bezier stroke rendering
 * - Non-destructive, stroke-based eraser
 * - Normalized (0.0..1.0) coordinates for resolution and zoom independence
 */
@Composable
fun InkCanvasOverlay(
    isPenModeActive: Boolean,
    activeTool: InkToolType,
    selectedColor: Color,
    selectedThickness: PenThickness,
    stylusOnlyMode: Boolean,
    strokes: List<RenderableStroke>,
    onStrokeCompleted: (RenderableStroke) -> Unit,
    onStrokesErased: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var currentPoints = remember { mutableStateListOf<InkPoint>() }
    var currentTool by remember { mutableStateOf(activeTool) }
    var currentColor by remember { mutableStateOf(selectedColor) }
    var currentWidthDp by remember { mutableStateOf(selectedThickness.penDp.value) }

    // Collect IDs of strokes erased during an active eraser drag
    val erasedStrokeIdsThisDrag = remember { mutableStateListOf<String>() }

    // If pen mode is not active, we simply render existing strokes (non-interactive, no touch consumption)
    if (!isPenModeActive) {
        Canvas(modifier = modifier.fillMaxSize().testTag("ink_display_canvas")) {
            for (stroke in strokes) {
                drawRenderableStroke(stroke, size.width, size.height, density.density)
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isPenModeActive, activeTool, selectedColor, selectedThickness, stylusOnlyMode, strokes) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val downChange = downEvent.changes.firstOrNull() ?: continue

                        val isStylus = downChange.type == PointerType.Stylus || downChange.type == PointerType.Eraser
                        val isHardwareEraser = downChange.type == PointerType.Eraser

                        // Palm rejection: If in stylus-only mode and touch is finger, do not intercept
                        if (stylusOnlyMode && !isStylus) {
                            continue
                        }

                        // Determine tool for this stroke
                        val effectiveTool = if (isHardwareEraser) {
                            InkToolType.ERASER
                        } else {
                            activeTool
                        }

                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        if (canvasWidth <= 0 || canvasHeight <= 0) continue

                        currentTool = effectiveTool
                        currentColor = selectedColor
                        currentWidthDp = if (effectiveTool == InkToolType.HIGHLIGHTER) {
                            selectedThickness.highlighterDp.value
                        } else {
                            selectedThickness.penDp.value
                        }

                        currentPoints.clear()
                        erasedStrokeIdsThisDrag.clear()

                        val normX = (downChange.position.x / canvasWidth).coerceIn(0f, 1f)
                        val normY = (downChange.position.y / canvasHeight).coerceIn(0f, 1f)
                        val pressure = if (downChange.pressure > 0f) downChange.pressure else 1.0f

                        if (effectiveTool == InkToolType.ERASER) {
                            // Check eraser hit at initial down
                            checkAndEraseStrokes(normX, normY, canvasWidth, canvasHeight, strokes, erasedStrokeIdsThisDrag)
                        } else {
                            currentPoints.add(InkPoint(normX, normY, pressure))
                        }
                        downChange.consume()

                        // Drag loop
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }

                            val curNormX = (change.position.x / canvasWidth).coerceIn(0f, 1f)
                            val curNormY = (change.position.y / canvasHeight).coerceIn(0f, 1f)
                            val curPressure = if (change.pressure > 0f) change.pressure else 1.0f

                            if (effectiveTool == InkToolType.ERASER) {
                                checkAndEraseStrokes(curNormX, curNormY, canvasWidth, canvasHeight, strokes, erasedStrokeIdsThisDrag)
                            } else {
                                currentPoints.add(InkPoint(curNormX, curNormY, curPressure))
                            }
                            change.consume()
                        }

                        // Up action
                        if (effectiveTool == InkToolType.ERASER) {
                            if (erasedStrokeIdsThisDrag.isNotEmpty()) {
                                onStrokesErased(erasedStrokeIdsThisDrag.toList())
                                erasedStrokeIdsThisDrag.clear()
                            }
                        } else if (currentPoints.size >= 1) {
                            val stroke = RenderableStroke(
                                id = UUID.randomUUID().toString(),
                                toolType = effectiveTool,
                                color = currentColor,
                                strokeWidthDp = currentWidthDp,
                                alpha = if (effectiveTool == InkToolType.HIGHLIGHTER) 0.38f else 1.0f,
                                points = currentPoints.toList()
                            )
                            onStrokeCompleted(stroke)
                            currentPoints.clear()
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ink_active_canvas")
        ) {
            val width = size.width
            val height = size.height
            val d = density.density

            // 1. Draw all saved strokes on this page (excluding those actively being erased in this drag)
            for (stroke in strokes) {
                if (stroke.id !in erasedStrokeIdsThisDrag) {
                    drawRenderableStroke(stroke, width, height, d)
                }
            }

            // 2. Draw in-progress stroke
            if (currentPoints.isNotEmpty() && currentTool != InkToolType.ERASER) {
                val activeStroke = RenderableStroke(
                    id = "active_draft",
                    toolType = currentTool,
                    color = currentColor,
                    strokeWidthDp = currentWidthDp,
                    alpha = if (currentTool == InkToolType.HIGHLIGHTER) 0.38f else 1.0f,
                    points = currentPoints.toList()
                )
                drawRenderableStroke(activeStroke, width, height, d)
            }
        }
    }
}

/**
 * Checks if the eraser point touches any stroke and marks it erased.
 */
private fun checkAndEraseStrokes(
    normX: Float,
    normY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    strokes: List<RenderableStroke>,
    erasedList: MutableList<String>
) {
    val pxX = normX * canvasWidth
    val pxY = normY * canvasHeight
    val thresholdPx = 28f // Eraser touch radius in pixels

    for (stroke in strokes) {
        if (stroke.id in erasedList) continue
        for (pt in stroke.points) {
            val ptX = pt.x * canvasWidth
            val ptY = pt.y * canvasHeight
            val dx = ptX - pxX
            val dy = ptY - pxY
            if (dx * dx + dy * dy <= thresholdPx * thresholdPx) {
                erasedList.add(stroke.id)
                break
            }
        }
    }
}

/**
 * Draws a smooth vector stroke with Bezier curve smoothing and pressure scaling.
 */
private fun DrawScope.drawRenderableStroke(
    stroke: RenderableStroke,
    width: Float,
    height: Float,
    densityMultiplier: Float
) {
    val points = stroke.points
    if (points.isEmpty()) return

    val strokeColor = stroke.color.copy(alpha = stroke.alpha)
    val baseWidthPx = stroke.strokeWidthDp * densityMultiplier

    if (points.size == 1) {
        // Single dot
        val p = points[0]
        val avgPressure = p.pressure.coerceIn(0.4f, 1.6f)
        val radius = (baseWidthPx * avgPressure) / 2f
        drawCircle(
            color = strokeColor,
            radius = radius,
            center = Offset(p.x * width, p.y * height)
        )
        return
    }

    // Connect points using quadratic bezier smoothing
    val path = Path()
    val p0 = points[0]
    path.moveTo(p0.x * width, p0.y * height)

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val prevX = prev.x * width
        val prevY = prev.y * height
        val currX = curr.x * width
        val currY = curr.y * height

        val midX = (prevX + currX) / 2f
        val midY = (prevY + currY) / 2f

        path.quadraticTo(prevX, prevY, midX, midY)
    }

    val last = points.last()
    path.lineTo(last.x * width, last.y * height)

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = baseWidthPx,
            cap = if (stroke.toolType == InkToolType.HIGHLIGHTER) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
