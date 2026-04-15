package com.readflow.app.data.repository

import com.readflow.app.domain.model.Annotation
import com.readflow.app.domain.model.AnnotationType
import com.readflow.app.domain.model.AnchorMeta
import com.readflow.app.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 标注和笔记 Repository
 */
@Singleton
class AnnotationRepository @Inject constructor() {

    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations = _annotations.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes = _notes.asStateFlow()

    init {
        // 加载示例数据
        loadSampleData()
    }

    private fun loadSampleData() {
        val sampleAnnotations = listOf(
            Annotation(
                id = "ann_1",
                documentId = "doc_1",
                pageIndex = 5,
                type = AnnotationType.HIGHLIGHT,
                color = "#FFEB3B",
                quote = "Android Jetpack Compose is a modern UI toolkit",
                anchorMeta = AnchorMeta(pageX = 50f, pageY = 100f, width = 300f, height = 20f)
            ),
            Annotation(
                id = "ann_2",
                documentId = "doc_1",
                pageIndex = 12,
                type = AnnotationType.UNDERLINE,
                color = "#03A9F4",
                quote = "State management is crucial in reactive apps",
                anchorMeta = AnchorMeta(pageX = 30f, pageY = 200f, width = 280f, height = 20f)
            ),
            Annotation(
                id = "ann_3",
                documentId = "doc_2",
                pageIndex = 8,
                type = AnnotationType.HIGHLIGHT,
                color = "#8BC34A",
                quote = "Coroutines provide a simple way to manage async operations",
                anchorMeta = AnchorMeta(pageX = 20f, pageY = 150f, width = 350f, height = 20f)
            )
        )

        val sampleNotes = listOf(
            Note(
                id = "note_1",
                documentId = "doc_1",
                pageIndex = 5,
                quote = "Android Jetpack Compose is a modern UI toolkit",
                noteText = "这里提到了 Compose，需要深入学习其声明式编程模型"
            ),
            Note(
                id = "note_2",
                documentId = "doc_2",
                pageIndex = 15,
                noteText = "协程是 Kotlin 处理异步编程的核心，需要掌握其调度器和上下文"
            )
        )

        _annotations.value = sampleAnnotations
        _notes.value = sampleNotes
    }

    /**
     * 获取文档的所有标注
     */
    fun getAnnotationsForDocument(documentId: String): Flow<List<Annotation>> {
        return _annotations.map { annotations ->
            annotations.filter { it.documentId == documentId }
        }
    }

    /**
     * 获取文档的所有笔记
     */
    fun getNotesForDocument(documentId: String): Flow<List<Note>> {
        return _notes.map { notes ->
            notes.filter { it.documentId == documentId }
        }
    }

    /**
     * 添加标注
     */
    suspend fun addAnnotation(
        documentId: String,
        pageIndex: Int,
        type: AnnotationType,
        color: String,
        quote: String,
        anchorMeta: AnchorMeta
    ): Annotation {
        val annotation = Annotation(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            pageIndex = pageIndex,
            type = type,
            color = color,
            quote = quote,
            anchorMeta = anchorMeta
        )
        _annotations.value = _annotations.value + annotation
        return annotation
    }

    /**
     * 删除标注
     */
    suspend fun deleteAnnotation(annotationId: String) {
        _annotations.value = _annotations.value.filter { it.id != annotationId }
    }

    /**
     * 更新标注颜色
     */
    suspend fun updateAnnotationColor(annotationId: String, color: String) {
        _annotations.value = _annotations.value.map { ann ->
            if (ann.id == annotationId) ann.copy(color = color, updatedAt = System.currentTimeMillis())
            else ann
        }
    }

    /**
     * 添加笔记
     */
    suspend fun addNote(
        documentId: String,
        pageIndex: Int,
        quote: String?,
        anchorMeta: AnchorMeta?,
        noteText: String
    ): Note {
        val note = Note(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            pageIndex = pageIndex,
            quote = quote,
            anchorMeta = anchorMeta,
            noteText = noteText
        )
        _notes.value = _notes.value + note
        return note
    }

    /**
     * 更新笔记
     */
    suspend fun updateNote(noteId: String, noteText: String) {
        _notes.value = _notes.value.map { note ->
            if (note.id == noteId) note.copy(
                noteText = noteText,
                updatedAt = System.currentTimeMillis()
            ) else note
        }
    }

    /**
     * 删除笔记
     */
    suspend fun deleteNote(noteId: String) {
        _notes.value = _notes.value.filter { it.id != noteId }
    }

    /**
     * 获取页面上的标注
     */
    fun getAnnotationsForPage(documentId: String, pageIndex: Int): Flow<List<Annotation>> {
        return _annotations.map { annotations ->
            annotations.filter {
                it.documentId == documentId && it.pageIndex == pageIndex
            }
        }
    }
}
