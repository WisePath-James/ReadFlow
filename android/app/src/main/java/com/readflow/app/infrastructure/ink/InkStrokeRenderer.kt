package com.readflow.app.infrastructure.ink

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.readflow.app.domain.model.InkTool
import com.readflow.app.domain.model.Point
import com.readflow.app.domain.model.Stroke

/**
 * 手写笔渲染器
 * 将 Stroke 数据渲染为 Compose Path
 */
object InkStrokeRenderer {

    fun strokeToPath(stroke: Stroke): Path {
        val path = Path()
        val points = stroke.points

        if (points.isEmpty()) return path

        if (points.size == 1) {
            // Single point - draw a dot
            path.addOval(
                androidx.compose.ui.geometry.Rect(
                    points[0].x - stroke.width / 2,
                    points[0].y - stroke.width / 2,
                    points[0].x + stroke.width / 2,
                    points[0].y + stroke.width / 2
                )
            )
            return path
        }

        // Start from first point
        path.moveTo(points[0].x, points[0].y)

        // Use quadratic bezier for smooth curves
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            if (i < points.size - 1) {
                val next = points[i + 1]
                val midX = (prev.x + curr.x + next.x) / 3f
                val midY = (prev.y + curr.y + next.y) / 3f
                path.quadraticBezierTo(
                    curr.x, curr.y,
                    midX, midY
                )
            } else {
                path.lineTo(curr.x, curr.y)
            }
        }

        return path
    }

    fun strokesToPaths(strokes: List<Stroke>): List<Pair<Stroke, Path>> {
        return strokes.map { stroke ->
            stroke to strokeToPath(stroke)
        }
    }

    fun getStrokeColor(stroke: Stroke): Color {
        return try {
            Color(android.graphics.Color.parseColor(stroke.color))
        } catch (e: Exception) {
            Color.Black
        }
    }

    fun getHighlightPath(stroke: Stroke): Path {
        // 高亮工具使用较宽的半透明笔触
        val path = Path()
        val points = stroke.points

        if (points.size < 2) return path

        val halfWidth = stroke.width * 3 // 高亮宽度

        // Create a filled path for highlighting
        path.moveTo(points[0].x, points[0].y - halfWidth)

        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y - halfWidth)
        }

        // Return along bottom edge
        for (i in points.size - 1 downTo 0) {
            path.lineTo(points[i].x, points[i].y + halfWidth)
        }

        path.close()
        return path
    }
}
