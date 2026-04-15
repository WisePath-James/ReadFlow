package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 手写批注模型 - Android 平板专用
 */
@Serializable
data class HandwritingAnnotation(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val drawingData: List<Stroke>,
    val anchorMeta: AnchorMeta,
    val toolType: InkTool = InkTool.PEN,
    val color: String = "#000000",
    val strokeWidth: Float = 2f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Stroke(
    val points: List<Point>,
    val tool: InkTool = InkTool.PEN,
    val color: String = "#000000",
    val width: Float = 2f,
    val alpha: Float = 1f
)

@Serializable
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class InkTool {
    PEN,
    PENCIL,
    HIGHLIGHTER,
    ERASER,
    LASSO
}
