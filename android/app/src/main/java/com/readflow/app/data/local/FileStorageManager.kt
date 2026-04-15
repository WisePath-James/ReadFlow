package com.readflow.app.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地文件存储管理器
 */
@Singleton
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val documentsDir: File
        get() = File(context.filesDir, "documents").also { it.mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.cacheDir, "thumbnails").also { it.mkdirs() }

    private val exportsDir: File
        get() = File(context.cacheDir, "exports").also { it.mkdirs() }

    /**
     * 复制文件到本地存储
     */
    suspend fun copyFile(uri: Uri, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val extension = getFileExtension(uri, fileName)
            val uniqueFileName = "${UUID.randomUUID()}.$extension"
            val targetFile = File(documentsDir, uniqueFileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open input stream"))

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存文件
     */
    suspend fun saveFile(data: ByteArray, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(documentsDir, fileName)
            file.writeBytes(data)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除文件
     */
    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取文件
     */
    fun getFile(filePath: String): File? {
        val file = File(filePath)
        return if (file.exists()) file else null
    }

    /**
     * 检查文件是否存在
     */
    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /**
     * 保存缩略图
     */
    suspend fun saveThumbnail(documentId: String, pageIndex: Int, data: ByteArray): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val fileName = "${documentId}_page_${pageIndex}.jpg"
                val file = File(thumbnailsDir, fileName)
                file.writeBytes(data)
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 获取缩略图路径
     */
    fun getThumbnailPath(documentId: String, pageIndex: Int): String {
        return File(thumbnailsDir, "${documentId}_page_${pageIndex}.jpg").absolutePath
    }

    /**
     * 获取目录大小
     */
    fun getDocumentsSize(): Long {
        return documentsDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    /**
     * 清理缓存
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        thumbnailsDir.listFiles()?.forEach { it.delete() }
        exportsDir.listFiles()?.forEach { it.delete() }
    }

    private fun getFileExtension(uri: Uri, fallbackName: String): String {
        val path = uri.path ?: fallbackName
        return path.substringAfterLast('.', "pdf")
    }
}
