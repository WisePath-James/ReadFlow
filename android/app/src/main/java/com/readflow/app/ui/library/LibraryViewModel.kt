package com.readflow.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readflow.app.data.repository.DocumentRepository
import com.readflow.app.domain.model.Document
import com.readflow.app.domain.model.Folder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val documents: List<Document> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val recentDocuments: List<Document> = emptyList(),
    val pinnedDocuments: List<Document> = emptyList(),
    val documentsInFolder: List<Document> = emptyList(),
    val searchResults: List<Document> = emptyList(),
    val selectedFolderId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(
                documentRepository.documents,
                documentRepository.folders,
                documentRepository.recentDocuments,
                documentRepository.pinnedDocuments,
                documentRepository.documentsInFolder(_uiState.value.selectedFolderId),
                _searchQuery
            ) { documents, folders, recent, pinned, inFolder, query ->
                val searchResults = if (query.isNotEmpty()) {
                    documents.filter { it.title.contains(query, ignoreCase = true) }
                } else emptyList()

                LibraryUiState(
                    documents = documents,
                    folders = folders,
                    recentDocuments = recent,
                    pinnedDocuments = pinned,
                    documentsInFolder = inFolder,
                    searchResults = searchResults,
                    selectedFolderId = _uiState.value.selectedFolderId
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectFolder(folderId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedFolderId = folderId)
            documentRepository.getDocumentsInFolder(folderId).collect { docs ->
                _uiState.value = _uiState.value.copy(documentsInFolder = docs)
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

    fun moveDocument(documentId: String, folderId: String?) {
        viewModelScope.launch {
            documentRepository.moveDocumentToFolder(documentId, folderId)
        }
    }

    fun togglePin(documentId: String) {
        viewModelScope.launch {
            documentRepository.togglePin(documentId)
        }
    }
}
