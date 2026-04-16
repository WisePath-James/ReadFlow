package com.readflow.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readflow.app.data.repository.DocumentRepository
import com.readflow.app.domain.model.Document
import com.readflow.app.domain.model.FileType
import com.readflow.app.domain.model.Folder
import com.readflow.app.domain.model.ProcessingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val documents: List<Document> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val recentDocuments: List<Document> = emptyList(),
    val pinnedDocuments: List<Document> = emptyList(),
    val searchResults: List<Document> = emptyList(),
    val selectedFolderId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            documentRepository.documents.collect { documents ->
                _uiState.value = _uiState.value.copy(
                    documents = documents,
                    recentDocuments = documents.take(5),
                    pinnedDocuments = documents.filter { it.isPinned }
                )
            }
        }

        viewModelScope.launch {
            documentRepository.folders.collect { folders ->
                _uiState.value = _uiState.value.copy(folders = folders)
            }
        }
    }

    fun selectFolder(folderId: String?) {
        _uiState.value = _uiState.value.copy(selectedFolderId = folderId)
        viewModelScope.launch {
            documentRepository.getDocumentsInFolder(folderId).collect { docs ->
                _uiState.value = _uiState.value.copy(documents = docs)
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                documentRepository.searchDocuments(query).collect { results ->
                    _uiState.value = _uiState.value.copy(searchResults = results)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    fun createFolder(name: String, parentId: String? = null, color: String = "#6750A4") {
        viewModelScope.launch {
            documentRepository.createFolder(name, parentId, color)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            documentRepository.deleteFolder(folderId)
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId)
        }
    }

    fun moveDocument(documentId: String, targetFolderId: String?) {
        viewModelScope.launch {
            documentRepository.moveDocumentToFolder(documentId, targetFolderId)
        }
    }

    fun togglePin(documentId: String) {
        viewModelScope.launch {
            documentRepository.togglePin(documentId)
        }
    }

    fun importDocument(filePath: String, fileName: String, fileType: FileType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, uploadProgress = 0f)

            try {
                _uiState.value = _uiState.value.copy(uploadProgress = 0.3f)

                val document = documentRepository.addDocument(
                    title = fileName.substringBeforeLast("."),
                    filePath = filePath,
                    fileType = fileType
                )

                _uiState.value = _uiState.value.copy(uploadProgress = 0.8f)

                // Simulate processing
                documentRepository.updateDocumentStatus(
                    document.id,
                    ProcessingStatus.COMPLETED
                )

                _uiState.value = _uiState.value.copy(uploadProgress = 1f)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Upload failed"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadProgress = 0f
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
