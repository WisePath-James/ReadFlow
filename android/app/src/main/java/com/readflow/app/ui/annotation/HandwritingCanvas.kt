package com.readflow.app.ui.annotation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.readflow.app.domain.model.InkTool
import com.readflow.app.domain.model.Stroke as InkStroke
import com.readflow.app.infrastructure.ink.InkStrokeManager
import com.readflow.app.infrastructure.ink.InkStrokeRenderer
import javax.inject.Inject

/**
 * 手写笔批注画布
 * Android 平板专用 - 支持手写笔和手指触控
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingCanvas(
    modifier: Modifier = Modifier,
    strokeManager: InkStrokeManager,
    currentTool: InkTool,
    currentColor: Color,
    currentWidth: Float,
    isEnabled: Boolean = true,
    onStrokeComplete: (List<InkStroke>) -> Unit = {}
) {
    var currentPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var isDrawing by remember { mutableStateOf(false) }

    val strokes = strokeManager.getAllStrokes()
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEnabled, currentTool, currentColor, currentWidth) {
                    if (!isEnabled) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            isDrawing = true
                            strokeManager.startStroke(
                                x = offset.x,
                                y = offset.y,
                                pressure = 1f
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            strokeManager.continueStroke(
                                x = change.position.x,
                                y = change.position.y,
                                pressure = 1f
                            )
                        },
                        onDragEnd = {
                            isDrawing = false
                            strokeManager.endStroke()
                            onStrokeComplete(strokeManager.getAllStrokes())
                        }
                    )
                }
        ) {
            // Draw completed strokes
            strokes.forEach { stroke ->
                val path = InkStrokeRenderer.strokeToPath(stroke)
                val color = InkStrokeRenderer.getStrokeColor(stroke)
                val alpha = if (stroke.tool == InkTool.HIGHLIGHTER) 0.4f else 1f

                when (stroke.tool) {
                    InkTool.HIGHLIGHTER -> {
                        // Draw as filled path with transparency
                        drawPath(
                            path = InkStrokeRenderer.getHighlightPath(stroke),
                            color = color.copy(alpha = alpha)
                        )
                    }
                    else -> {
                        // Draw as stroke
                        drawPath(
                            path = path,
                            color = color.copy(alpha = alpha),
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Draw current stroke in progress
            val currentStrokePoints = strokeManager.getCurrentStrokePoints()
            if (currentStrokePoints.isNotEmpty()) {
                val path = Path()
                val firstPoint = currentStrokePoints.first()
                path.moveTo(firstPoint.x, firstPoint.y)

                currentStrokePoints.drop(1).forEach { point ->
                    path.lineTo(point.x, point.y)
                }

                val alpha = if (currentTool == InkTool.HIGHLIGHTER) 0.4f else 1f

                if (currentTool == InkTool.HIGHLIGHTER) {
                    // For highlighter, we would need to draw a filled area
                    // Simplified here as a thick stroke
                    drawPath(
                        path = path,
                        color = currentColor.copy(alpha = alpha),
                        style = Stroke(
                            width = currentWidth * 3,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    drawPath(
                        path = path,
                        color = currentColor.copy(alpha = alpha),
                        style = Stroke(
                            width = currentWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * 手写笔工具栏
 * Android 平板专用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingToolbar(
    currentTool: InkTool,
    currentColor: Color,
    currentWidth: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolChange: (InkTool) -> Unit,
    onColorChange: (Color) -> Unit,
    onWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color.Black,
        Color.Blue,
        Color.Red,
        Color.Green,
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
    )

    val widths = listOf(1f, 2f, 4f, 8f)

    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tool selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolButton(
                    icon = Icons.Default.Edit,
                    label = "Pen",
                    isSelected = currentTool == InkTool.PEN,
                    onClick = { onToolChange(InkTool.PEN) }
                )
                ToolButton(
                    icon = Icons.Default.Create,
                    label = "Pencil",
                    isSelected = currentTool == InkTool.PENCIL,
                    onClick = { onToolChange(InkTool.PENCIL) }
                )
                ToolButton(
                    icon = Icons.Default.Highlight,
                    label = "Marker",
                    isSelected = currentTool == InkTool.HIGHLIGHTER,
                    onClick = { onToolChange(InkTool.HIGHLIGHTER) }
                )
                ToolButton(
                    icon = Icons.Default.Delete,
                    label = "Eraser",
                    isSelected = currentTool == InkTool.ERASER,
                    onClick = { onToolChange(InkTool.ERASER) }
                )
                ToolButton(
                    icon = Icons.Default.SelectAll,
                    label = "Lasso",
                    isSelected = currentTool == InkTool.LASSO,
                    onClick = { onToolChange(InkTool.LASSO) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Color selection
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                colors.forEach { color ->
                    ColorButton(
                        color = color,
                        isSelected = currentColor == color,
                        onClick = { onColorChange(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Width selection
            Text(
                text = "Width",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                widths.forEach { width ->
                    WidthButton(
                        width = width,
                        isSelected = currentWidth == width,
                        onClick = { onWidthChange(width) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Undo/Redo/Clear
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all")
                }
            }

            // Close button
            TextButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Done")
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        )
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = color,
        border = if (isSelected) {
            ButtonDefaults.outlinedButtonBorder(enabled = true)
        } else null,
        modifier = Modifier.size(32.dp)
    ) {}
}

@Composable
private fun WidthButton(
    width: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent,
        modifier = Modifier.size(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black,
                modifier = Modifier.size((width * 4).dp.coerceAtMost(20.dp))
            ) {}
        }
    }
}

/**
 * 手写笔批注面板
 * 包含画布和工具栏
 */
@Composable
fun HandwritingPanel(
    strokeManager: InkStrokeManager,
    isVisible: Boolean,
    onClose: () -> Unit,
    onSave: (List<InkStroke>) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTool by remember { mutableStateOf(InkTool.PEN) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentWidth by remember { mutableFloatStateOf(2f) }

    if (isVisible) {
        Row(modifier = modifier.fillMaxSize()) {
            // Drawing canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                HandwritingCanvas(
                    strokeManager = strokeManager,
                    currentTool = currentTool,
                    currentColor = currentColor,
                    currentWidth = currentWidth,
                    isEnabled = true,
                    onStrokeComplete = { strokes ->
                        // Auto-save or update
                    }
                )
            }

            // Toolbar
            HandwritingToolbar(
                currentTool = currentTool,
                currentColor = currentColor,
                currentWidth = currentWidth,
                canUndo = strokeManager.canUndo(),
                canRedo = strokeManager.canRedo(),
                onToolChange = { tool ->
                    currentTool = tool
                    strokeManager.setTool(tool, currentColor, currentWidth)
                },
                onColorChange = { color ->
                    currentColor = color
                    strokeManager.setTool(currentTool, color, currentWidth)
                },
                onWidthChange = { width ->
                    currentWidth = width
                    strokeManager.setTool(currentTool, currentColor, width)
                },
                onUndo = { strokeManager.undo() },
                onRedo = { strokeManager.redo() },
                onClear = { strokeManager.clearAll() },
                onClose = {
                    onSave(strokeManager.getAllStrokes())
                    onClose()
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
