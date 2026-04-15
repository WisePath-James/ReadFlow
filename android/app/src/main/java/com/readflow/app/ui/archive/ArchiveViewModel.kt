package com.readflow.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readflow.app.data.repository.ArchiveRepository
import com.readflow.app.data.repository.DocumentRepository
import com.readflow.app.domain.model.Archive
import com.readflow.app.domain.model.Document
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentArchive(
    val document: Document,
    val archives: List<Archive>
)

data class ArchiveUiState(
    val archives: List<Archive> = emptyList(),
    val archivesByDocument: List<DocumentArchive> = emptyList(),
    val tags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        loadArchives()
        loadTags()
    }

    private fun loadArchives() {
        viewModelScope.launch {
            archiveRepository.archives.collect { archives ->
                updateArchivesList(archives)
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            archiveRepository.getAllTags().collect { tags ->
                _uiState.value = _uiState.value.copy(tags = tags)
            }
        }
    }

    private fun updateArchivesList(archives: List<Archive>) {
        val filtered = if (_uiState.value.selectedTag != null) {
            archives.filter { it.tag == _uiState.value.selectedTag }
        } else if (_uiState.value.searchQuery.isNotEmpty()) {
            archives.filter {
                it.question.contains(_uiState.value.searchQuery, ignoreCase = true) ||
                it.answer.contains(_uiState.value.searchQuery, ignoreCase = true)
            }
        } else {
            archives
        }

        // Group by document
        val byDocument = filtered.groupBy { it.documentId }
            .mapNotNull { (docId, docArchives) ->
                val document = documentRepository.getDocument(docId)
                document?.let { DocumentArchive(it, docArchives) }
            }

        _uiState.value = _uiState.value.copy(
            archives = filtered,
            archivesByDocument = byDocument
        )
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isEmpty()) {
                archiveRepository.archives.collect { updateArchivesList(it) }
            } else {
                archiveRepository.searchArchives(query).collect { updateArchivesList(it) }
            }
        }
    }

    fun selectTag(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
        viewModelScope.launch {
            if (tag == null) {
                archiveRepository.archives.collect { updateArchivesList(it) }
            } else {
                archiveRepository.getArchivesByTag(tag).collect { updateArchivesList(it) }
            }
        }
    }

    fun deleteArchive(archiveId: String) {
        viewModelScope.launch {
            archiveRepository.deleteArchive(archiveId)
        }
    }
}
