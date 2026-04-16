package com.readflow.app.infrastructure.ink

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.readflow.app.domain.model.InkTool
import com.readflow.app.domain.model.Point
import com.readflow.app.domain.model.Stroke
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 手写笔墨迹管理器
 * 处理 Android 平板的手写笔绘制逻辑
 */
@Singleton
class InkStrokeManager @Inject constructor() {

    private val strokes = mutableListOf<Stroke>()
    private val redoStack = mutableListOf<Stroke>()
    private var currentStrokePoints = mutableListOf<Point>()

    private var currentTool: InkTool = InkTool.PEN
    private var currentColor: Color = Color.Black
    private var currentWidth: Float = 2f

    fun setTool(tool: InkTool, color: Color, width: Float) {
        currentTool = tool
        currentColor = color
        currentWidth = width
    }

    fun startStroke(x: Float, y: Float, pressure: Float = 1f) {
        if (currentTool == InkTool.ERASER) {
            eraseAt(x, y)
            return
        }

        currentStrokePoints.clear()
        currentStrokePoints.add(
            Point(
                x = x,
                y = y,
                pressure = pressure,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun continueStroke(x: Float, y: Float, pressure: Float = 1f) {
        if (currentTool == InkTool.ERASER) {
            eraseAt(x, y)
            return
        }

        currentStrokePoints.add(
            Point(
                x = x,
                y = y,
                pressure = pressure,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun endStroke() {
        if (currentTool == InkTool.ERASER) {
            return
        }

        if (currentStrokePoints.size >= 2) {
            val stroke = Stroke(
                points = currentStrokePoints.toList(),
                tool = currentTool,
                color = currentColor.toHexString(),
                width = currentWidth,
                alpha = if (currentTool == InkTool.HIGHLIGHTER) 0.5f else 1f
            )
            strokes.add(stroke)
            redoStack.clear()
        }
        currentStrokePoints.clear()
    }

    fun getCurrentStrokePoints(): List<Point> = currentStrokePoints.toList()

    fun getAllStrokes(): List<Stroke> = strokes.toList()

    fun undo(): Boolean {
        if (strokes.isNotEmpty()) {
            redoStack.add(strokes.removeLast())
            return true
        }
        return false
    }

    fun redo(): Boolean {
        if (redoStack.isNotEmpty()) {
            strokes.add(redoStack.removeLast())
            return true
        }
        return false
    }

    fun canUndo(): Boolean = strokes.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    private fun eraseAt(x: Float, y: Float) {
        val eraseRadius = currentWidth * 5 // 橡皮擦大小
        strokes.removeAll { stroke ->
            stroke.points.any { point ->
                val dx = point.x - x
                val dy = point.y - y
                (dx * dx + dy * dy) <= (eraseRadius * eraseRadius)
            }
        }
    }

    fun eraseStroke(stroke: Stroke) {
        strokes.remove(stroke)
    }

    fun clearAll() {
        strokes.clear()
        redoStack.clear()
        currentStrokePoints.clear()
    }

    fun setStrokes(newStrokes: List<Stroke>) {
        strokes.clear()
        strokes.addAll(newStrokes)
        redoStack.clear()
    }

    private fun Color.toHexString(): String {
        return String.format("#%06X", (0xFFFFFF and this.toArgb()))
    }
}
