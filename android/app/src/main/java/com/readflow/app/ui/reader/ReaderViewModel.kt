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
import com.readflow.app.infrastructure.pdf.PDFRendererCore
import com.readflow.app.infrastructure.pdf.TextSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val pageIndex: Int,
    val context: String,
    val matchedText: String
)

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
    val error: String? = null,
    // Search state
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val currentSearchIndex: Int = 0,
    // Table of Contents
    val showToc: Boolean = false,
    val tableOfContents: List<TocItem> = emptyList()
)

data class TocItem(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val annotationRepository: AnnotationRepository,
    private val readingProgressRepository: ReadingProgressRepository,
    private val pdfRendererCore: PDFRendererCore
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

                // Open PDF
                val openResult = pdfRendererCore.openDocument(document.filePath)
                val pageCount = if (openResult.isSuccess) {
                    openResult.getOrNull() ?: document.pageCount
                } else {
                    document.pageCount
                }

                _uiState.value = _uiState.value.copy(
                    document = document,
                    pageCount = pageCount.coerceAtLeast(1)
                )

                // Load reading progress
                readingProgressRepository.getProgress(documentId).collectLatest { progress ->
                    if (progress != null) {
                        _uiState.value = _uiState.value.copy(
                            currentPage = progress.currentPage.coerceIn(0, pageCount - 1),
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

    // ==================== Search Functions ====================

    fun startSearch() {
        _uiState.value = _uiState.value.copy(
            isSearching = true,
            searchQuery = "",
            searchResults = emptyList(),
            currentSearchIndex = 0
        )
    }

    fun dismissSearch() {
        _uiState.value = _uiState.value.copy(
            isSearching = false,
            searchQuery = "",
            searchResults = emptyList(),
            currentSearchIndex = 0
        )
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                currentSearchIndex = 0
            )
            return
        }

        viewModelScope.launch {
            val results = mutableListOf<SearchResult>()
            
            // Search in PDF
            val pdfResults = pdfRendererCore.searchText(query)
            if (pdfResults.isSuccess) {
                results.addAll(
                    pdfResults.getOrNull()?.map { result ->
                        SearchResult(
                            pageIndex = result.pageIndex,
                            context = result.context,
                            matchedText = result.matchedText
                        )
                    } ?: emptyList()
                )
            }

            // Search in annotations
            val annotations = _uiState.value.annotations
            annotations.forEach { annotation ->
                if (annotation.quote.contains(query, ignoreCase = true)) {
                    results.add(
                        SearchResult(
                            pageIndex = annotation.pageIndex,
                            context = annotation.quote.take(100),
                            matchedText = query
                        )
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                searchResults = results,
                currentSearchIndex = 0
            )

            // Navigate to first result
            if (results.isNotEmpty()) {
                goToPage(results.first().pageIndex)
            }
        }
    }

    fun nextSearchResult() {
        val results = _uiState.value.searchResults
        if (results.isEmpty()) return
        
        val newIndex = (_uiState.value.currentSearchIndex + 1) % results.size
        _uiState.value = _uiState.value.copy(currentSearchIndex = newIndex)
        goToPage(results[newIndex].pageIndex)
    }

    fun previousSearchResult() {
        val results = _uiState.value.searchResults
        if (results.isEmpty()) return
        
        val newIndex = if (_uiState.value.currentSearchIndex <= 0) {
            results.size - 1
        } else {
            _uiState.value.currentSearchIndex - 1
        }
        _uiState.value = _uiState.value.copy(currentSearchIndex = newIndex)
        goToPage(results[newIndex].pageIndex)
    }

    fun toggleToc() {
        val showToc = !_uiState.value.showToc
        _uiState.value = _uiState.value.copy(showToc = showToc)
        
        if (showToc && _uiState.value.tableOfContents.isEmpty()) {
            loadTableOfContents()
        }
    }

    private fun loadTableOfContents() {
        viewModelScope.launch {
            // Generate simple table of contents from page structure
            val pageCount = _uiState.value.pageCount
            val tocItems = mutableListOf<TocItem>()
            
            // Add page markers as TOC items
            val interval = maxOf(1, pageCount / 20)
            for (i in 0 until pageCount step interval) {
                tocItems.add(
                    TocItem(
                        title = "Page ${i + 1}",
                        pageIndex = i,
                        level = 0
                    )
                )
            }
            
            // Add last page
            if (pageCount > 1 && tocItems.last().pageIndex != pageCount - 1) {
                tocItems.add(
                    TocItem(
                        title = "Page $pageCount",
                        pageIndex = pageCount - 1,
                        level = 0
                    )
                )
            }

            _uiState.value = _uiState.value.copy(tableOfContents = tocItems)
        }
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
        pdfRendererCore.close()
    }
}
