package com.readflow.app.infrastructure.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 原生 Android PDF 渲染器
 * 使用 Android 内置 PdfRenderer API
 */
@Singleton
class PDFRendererCore @Inject constructor() {

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pageCount: Int = 0

    suspend fun openDocument(filePath: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            close()
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $filePath"))
            }
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            pageCount = pdfRenderer!!.pageCount
            Result.success(pageCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renderPage(
        pageIndex: Int,
        width: Int,
        height: Int,
        isHighQuality: Boolean = false
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val renderer = pdfRenderer
                ?: return@withContext Result.failure(Exception("PDF not opened"))

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                return@withContext Result.failure(Exception("Invalid page index"))
            }

            renderer.openPage(pageIndex).use { page ->
                val scale = if (isHighQuality) 2.0f else 1.0f
                val bitmapWidth = (width * scale).toInt()
                val bitmapHeight = (height * scale).toInt()

                val bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    bitmapHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)

                val renderParams = PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                page.render(
                    bitmap,
                    null,
                    null,
                    renderParams
                )

                Result.success(bitmap)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPageText(pageIndex: Int): Result<String> = withContext(Dispatchers.IO) {
        // Android PdfRenderer doesn't support text extraction directly
        // This would need a library like PDF.js or iText for text extraction
        Result.success("")
    }

    fun getPageCount(): Int = pageCount

    fun close() {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            // Ignore close errors
        }
        pdfRenderer = null
        fileDescriptor = null
        pageCount = 0
    }
}
