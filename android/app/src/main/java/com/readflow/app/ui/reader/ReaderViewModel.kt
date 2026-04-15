package com.readflow.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readflow.app.data.repository.AnnotationRepository
import com.readflow.app.data.repository.DocumentRepository
import com.readflow.app.data.repository.ReadingProgressRepository
import com.readflow.app.domain.model.AnchorMeta
import com.readflow.app.domain.model.Annotation
import com.readflow.app.domain.model.AnnotationType
import com.readflow.app.domain.model.Document
import com.readflow.app.domain.model.Note
import com.readflow.app.domain.model.ReadingMode
import com.readflow.app.domain.model.ReadingTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val document: Document? = null,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val zoomLevel: Float = 1f,
    val readingMode: ReadingMode = ReadingMode.CONTINUOUS,
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val annotations: List<Annotation> = emptyList(),
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val annotationRepository: AnnotationRepository,
    private val readingProgressRepository: ReadingProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentDocumentId: String? = null

    fun loadDocument(documentId: String) {
        currentDocumentId = documentId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load document
                val document = documentRepository.getDocument(documentId)
                if (document == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Document not found"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    document = document,
                    pageCount = document.pageCount.coerceAtLeast(1)
                )

                // Load reading progress
                readingProgressRepository.getProgress(documentId).collectLatest { progress ->
                    if (progress != null) {
                        _uiState.value = _uiState.value.copy(
                            currentPage = progress.currentPage,
                            zoomLevel = progress.zoomLevel,
                            readingMode = progress.readingMode,
                            theme = progress.theme
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load document"
                )
            }

            // Load annotations
            annotationRepository.getAnnotationsForDocument(documentId).collectLatest { annotations ->
                _uiState.value = _uiState.value.copy(
                    annotations = annotations,
                    isLoading = false
                )
            }
        }
    }

    fun goToPage(page: Int) {
        val pageCount = _uiState.value.pageCount
        if (page in 0 until pageCount) {
            _uiState.value = _uiState.value.copy(currentPage = page)
            saveProgress()
        }
    }

    fun nextPage() {
        goToPage(_uiState.value.currentPage + 1)
    }

    fun previousPage() {
        goToPage(_uiState.value.currentPage - 1)
    }

    fun toggleReadingMode() {
        val newMode = when (_uiState.value.readingMode) {
            ReadingMode.CONTINUOUS -> ReadingMode.SINGLE_PAGE
            ReadingMode.SINGLE_PAGE -> ReadingMode.REFLOW
            ReadingMode.REFLOW -> ReadingMode.CONTINUOUS
        }
        _uiState.value = _uiState.value.copy(readingMode = newMode)
        saveProgress()
    }

    fun setReadingMode(mode: ReadingMode) {
        _uiState.value = _uiState.value.copy(readingMode = mode)
        saveProgress()
    }

    fun toggleTheme() {
        val newTheme = when (_uiState.value.theme) {
            ReadingTheme.LIGHT -> ReadingTheme.DARK
            ReadingTheme.DARK -> ReadingTheme.SEPIA
            ReadingTheme.SEPIA -> ReadingTheme.LIGHT
        }
        _uiState.value = _uiState.value.copy(theme = newTheme)
        saveProgress()
    }

    fun setTheme(theme: ReadingTheme) {
        _uiState.value = _uiState.value.copy(theme = theme)
        saveProgress()
    }

    fun setZoom(zoom: Float) {
        _uiState.value = _uiState.value.copy(zoomLevel = zoom.coerceIn(0.5f, 3f))
        saveProgress()
    }

    fun toggleZoom() {
        val current = _uiState.value.zoomLevel
        val newZoom = if (current < 1.5f) 2f else 1f
        setZoom(newZoom)
    }

    fun addAnnotation(
        documentId: String,
        pageIndex: Int,
        type: AnnotationType,
        color: String,
        quote: String
    ) {
        viewModelScope.launch {
            val anchorMeta = AnchorMeta(
                pageX = 0f,
                pageY = 0f,
                width = 0f,
                height = 0f
            )
            annotationRepository.addAnnotation(
                documentId = documentId,
                pageIndex = pageIndex,
                type = type,
                color = color,
                quote = quote,
                anchorMeta = anchorMeta
            )
        }
    }

    fun deleteAnnotation(annotationId: String) {
        viewModelScope.launch {
            annotationRepository.deleteAnnotation(annotationId)
        }
    }

    fun updateAnnotationColor(annotationId: String, color: String) {
        viewModelScope.launch {
            annotationRepository.updateAnnotationColor(annotationId, color)
        }
    }

    fun addNote(
        documentId: String,
        pageIndex: Int,
        quote: String?,
        noteText: String
    ) {
        viewModelScope.launch {
            val anchorMeta = if (quote != null) {
                AnchorMeta(pageX = 0f, pageY = 0f)
            } else null

            annotationRepository.addNote(
                documentId = documentId,
                pageIndex = pageIndex,
                quote = quote,
                anchorMeta = anchorMeta,
                noteText = noteText
            )
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            annotationRepository.deleteNote(noteId)
        }
    }

    fun updateNote(noteId: String, noteText: String) {
        viewModelScope.launch {
            annotationRepository.updateNote(noteId, noteText)
        }
    }

    private fun saveProgress() {
        currentDocumentId?.let { documentId ->
            viewModelScope.launch {
                readingProgressRepository.updateProgress(
                    documentId = documentId,
                    currentPage = _uiState.value.currentPage,
                    zoomLevel = _uiState.value.zoomLevel,
                    readingMode = _uiState.value.readingMode,
                    theme = _uiState.value.theme
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
    }
}
