package com.readflow.app.data.repository

import com.readflow.app.domain.model.AISource
import com.readflow.app.domain.model.Archive
import com.readflow.app.domain.model.AnchorMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Archive 知识卡片 Repository
 */
@Singleton
class ArchiveRepository @Inject constructor() {

    private val _archives = MutableStateFlow<List<Archive>>(emptyList())
    val archives = _archives.asStateFlow()

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        val sampleArchives = listOf(
            Archive(
                id = "archive_1",
                documentId = "doc_1",
                pageIndex = 5,
                quote = "Android Jetpack Compose is a modern UI toolkit",
                anchorMeta = AnchorMeta(pageX = 50f, pageY = 100f),
                question = "什么是 Jetpack Compose?",
                answer = "Jetpack Compose 是 Google 推出的现代 Android UI 工具包,采用完全声明式的方式构建 UI。相比传统的 XML 布局方式,Compose 可以用更少的代码创建复杂的 UI。",
                tag = "核心概念"
            ),
            Archive(
                id = "archive_2",
                documentId = "doc_1",
                pageIndex = 12,
                quote = "State management is crucial in reactive apps",
                anchorMeta = AnchorMeta(pageX = 30f, pageY = 200f),
                question = "为什么状态管理很重要?",
                answer = "在响应式应用中,状态管理是核心问题。正确管理状态可以确保 UI 与数据保持同步,提升应用的可维护性和用户体验。",
                tag = "重要概念"
            ),
            Archive(
                id = "archive_3",
                documentId = "doc_2",
                pageIndex = 8,
                quote = "Coroutines provide a simple way to manage async operations",
                anchorMeta = AnchorMeta(pageX = 20f, pageY = 150f),
                question = "Kotlin 协程是什么?",
                answer = "协程是 Kotlin 提供的轻量级并发方案,相比线程,协程更轻量,可以更简单地处理异步操作。协程通过挂起函数实现非阻塞式编程。",
                tag = "异步编程"
            )
        )
        _archives.value = sampleArchives
    }

    /**
     * 获取文档的所有 Archive
     */
    fun getArchivesForDocument(documentId: String): Flow<List<Archive>> {
        return _archives.map { archives ->
            archives.filter { it.documentId == documentId }
        }
    }

    /**
     * 按标签获取 Archive
     */
    fun getArchivesByTag(tag: String): Flow<List<Archive>> {
        return _archives.map { archives ->
            archives.filter { it.tag == tag }
        }
    }

    /**
     * 获取所有标签
     */
    fun getAllTags(): Flow<List<String>> {
        return _archives.map { archives ->
            archives.mapNotNull { it.tag }.distinct().sorted()
        }
    }

    /**
     * 创建 Archive
     */
    suspend fun createArchive(
        documentId: String,
        pageIndex: Int,
        quote: String,
        anchorMeta: AnchorMeta,
        question: String,
        answer: String,
        tag: String? = null
    ): Archive {
        val archive = Archive(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            pageIndex = pageIndex,
            quote = quote,
            anchorMeta = anchorMeta,
            question = question,
            answer = answer,
            tag = tag
        )
        _archives.value = _archives.value + archive
        return archive
    }

    /**
     * 更新 Archive 标签
     */
    suspend fun updateArchiveTag(archiveId: String, tag: String?) {
        _archives.value = _archives.value.map { archive ->
            if (archive.id == archiveId) archive.copy() else archive
        }
    }

    /**
     * 删除 Archive
     */
    suspend fun deleteArchive(archiveId: String) {
        _archives.value = _archives.value.filter { it.id != archiveId }
    }

    /**
     * 搜索 Archive
     */
    fun searchArchives(query: String): Flow<List<Archive>> {
        return _archives.map { archives ->
            if (query.isBlank()) emptyList()
            else archives.filter { archive ->
                archive.question.contains(query, ignoreCase = true) ||
                archive.answer.contains(query, ignoreCase = true) ||
                archive.tag?.contains(query, ignoreCase = true) == true
            }
        }
    }

    /**
     * 获取最近的 Archive
     */
    fun getRecentArchives(limit: Int = 10): Flow<List<Archive>> {
        return _archives.map { archives ->
            archives.sortedByDescending { it.createdAt }.take(limit)
        }
    }
}
