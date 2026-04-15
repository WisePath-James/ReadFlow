package com.readflow.app.data.repository

import com.readflow.app.data.local.FileStorageManager
import com.readflow.app.data.local.PreferencesManager
import com.readflow.app.domain.model.Document
import com.readflow.app.domain.model.Folder
import com.readflow.app.domain.model.FolderTreeNode
import com.readflow.app.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文档和文件夹 Repository
 * 管理本地数据状态
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val fileStorageManager: FileStorageManager
) {
    // 内存中的文档和文件夹状态
    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents = _documents.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _recentDocuments = MutableStateFlow<List<Document>>(emptyList())
    val recentDocuments = _recentDocuments.asStateFlow()

    private val _pinnedDocuments = MutableStateFlow<List<Document>>(emptyList())
    val pinnedDocuments = _pinnedDocuments.asStateFlow()

    init {
        // 初始化示例数据
        loadSampleData()
    }

    private fun loadSampleData() {
        val sampleFolders = listOf(
            Folder(
                id = "folder_1",
                ownerId = "user_1",
                name = "技术文档",
                color = "#6750A4",
                documentCount = 3
            ),
            Folder(
                id = "folder_2",
                ownerId = "user_1",
                name = "学习资料",
                color = "#7D5260",
                documentCount = 5
            ),
            Folder(
                id = "folder_3",
                ownerId = "user_1",
                name = "工作文档",
                color = "#625B71",
                documentCount = 2
            )
        )

        val sampleDocuments = listOf(
            Document(
                id = "doc_1",
                ownerId = "user_1",
                folderId = "folder_1",
                title = "Android 开发指南",
                filePath = "/sample/doc1.pdf",
                fileType = com.readflow.app.domain.model.FileType.PDF,
                pageCount = 156,
                processingStatus = com.readflow.app.domain.model.ProcessingStatus.COMPLETED
            ),
            Document(
                id = "doc_2",
                ownerId = "user_1",
                folderId = "folder_1",
                title = "Kotlin 核心技术",
                filePath = "/sample/doc2.pdf",
                fileType = com.readflow.app.domain.model.FileType.PDF,
                pageCount = 89,
                processingStatus = com.readflow.app.domain.model.ProcessingStatus.COMPLETED
            ),
            Document(
                id = "doc_3",
                ownerId = "user_1",
                folderId = "folder_2",
                title = "机器学习入门",
                filePath = "/sample/doc3.pdf",
                fileType = com.readflow.app.domain.model.FileType.PDF,
                pageCount = 234,
                processingStatus = com.readflow.app.domain.model.ProcessingStatus.COMPLETED,
                isPinned = true
            ),
            Document(
                id = "doc_4",
                ownerId = "user_1",
                folderId = "folder_2",
                title = "深度学习实战",
                filePath = "/sample/doc4.pdf",
                fileType = com.readflow.app.domain.model.FileType.PDF,
                pageCount = 412,
                processingStatus = com.readflow.app.domain.model.ProcessingStatus.PROCESSING
            ),
            Document(
                id = "doc_5",
                ownerId = "user_1",
                folderId = null,
                title = "最近阅读的论文",
                filePath = "/sample/doc5.pdf",
                fileType = com.readflow.app.domain.model.FileType.PDF,
                pageCount = 12,
                processingStatus = com.readflow.app.domain.model.ProcessingStatus.COMPLETED
            )
        )

        _folders.value = sampleFolders
        _documents.value = sampleDocuments
        _recentDocuments.value = sampleDocuments.take(3)
        _pinnedDocuments.value = sampleDocuments.filter { it.isPinned }
    }

    /**
     * 获取文件夹树
     */
    fun getFolderTree(): List<FolderTreeNode> {
        val allFolders = _folders.value
        val buildTree: (Folder?, Int) -> List<FolderTreeNode> = { parent, depth ->
            allFolders
                .filter { it.parentFolderId == parent?.id }
                .map { folder ->
                    FolderTreeNode(
                        folder = folder,
                        children = buildTree(folder, depth + 1),
                        depth = depth
                    )
                }
        }
        return buildTree(null, 0)
    }

    /**
     * 获取文件夹中的文档
     */
    fun getDocumentsInFolder(folderId: String?): Flow<List<Document>> {
        return _documents.map { docs ->
            docs.filter { it.folderId == folderId }
        }
    }

    /**
     * 添加文档
     */
    suspend fun addDocument(
        title: String,
        filePath: String,
        fileType: com.readflow.app.domain.model.FileType,
        folderId: String? = null
    ): Document {
        val document = Document(
            id = UUID.randomUUID().toString(),
            ownerId = "user_1",
            folderId = folderId,
            title = title,
            filePath = filePath,
            fileType = fileType,
            pageCount = 0,
            processingStatus = com.readflow.app.domain.model.ProcessingStatus.PENDING
        )
        _documents.value = _documents.value + document
        return document
    }

    /**
     * 删除文档
     */
    suspend fun deleteDocument(documentId: String): Boolean {
        val doc = _documents.value.find { it.id == documentId } ?: return false
        fileStorageManager.deleteFile(doc.filePath)
        _documents.value = _documents.value.filter { it.id != documentId }
        return true
    }

    /**
     * 移动文档到文件夹
     */
    suspend fun moveDocumentToFolder(documentId: String, folderId: String?) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == documentId) doc.copy(folderId = folderId) else doc
        }
    }

    /**
     * 切换文档置顶状态
     */
    suspend fun togglePin(documentId: String) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == documentId) doc.copy(isPinned = !doc.isPinned) else doc
        }
        _pinnedDocuments.value = _documents.value.filter { it.isPinned }
    }

    /**
     * 创建文件夹
     */
    suspend fun createFolder(name: String, parentId: String? = null, color: String = "#6750A4"): Folder {
        val folder = Folder(
            id = UUID.randomUUID().toString(),
            ownerId = "user_1",
            parentFolderId = parentId,
            name = name,
            color = color
        )
        _folders.value = _folders.value + folder
        return folder
    }

    /**
     * 删除文件夹
     */
    suspend fun deleteFolder(folderId: String): Boolean {
        _folders.value = _folders.value.filter { it.id != folderId }
        // 将文件夹中的文档移到根目录
        _documents.value = _documents.value.map { doc ->
            if (doc.folderId == folderId) doc.copy(folderId = null) else doc
        }
        return true
    }

    /**
     * 搜索文档
     */
    fun searchDocuments(query: String): Flow<List<Document>> {
        return _documents.map { docs ->
            if (query.isBlank()) emptyList()
            else docs.filter {
                it.title.contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * 获取文档
     */
    fun getDocument(documentId: String): Document? {
        return _documents.value.find { it.id == documentId }
    }

    /**
     * 更新文档处理状态
     */
    suspend fun updateDocumentStatus(documentId: String, status: com.readflow.app.domain.model.ProcessingStatus) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == documentId) doc.copy(
                processingStatus = status,
                updatedAt = System.currentTimeMillis()
            ) else doc
        }
    }
}
