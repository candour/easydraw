package com.messark.easydraw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtils {

    suspend fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun renderPdfPages(context: Context, uri: Uri, maxPages: Int = 20): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val pageCount = minOf(renderer.pageCount, maxPages)
                for (i in 0 until pageCount) {
                    var page: PdfRenderer.Page? = null
                    try {
                        page = renderer.openPage(i)
                        // Thumbnails should be small to save memory
                        val width = 200
                        val height = (width.toFloat() / page.width * page.height).toInt()

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                    } finally {
                        page?.close()
                    }
                }
                renderer.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }

    suspend fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    renderer.close()
                    return@withContext null
                }
                var page: PdfRenderer.Page? = null
                val bitmap = try {
                    page = renderer.openPage(pageIndex)
                    // Limit max dimension to avoid OOM for very large PDFs
                    val maxDimension = 2048
                    var width = page.width
                    var height = page.height
                    if (width > maxDimension || height > maxDimension) {
                        val ratio = width.toFloat() / height
                        if (width > height) {
                            width = maxDimension
                            height = (width / ratio).toInt()
                        } else {
                            height = maxDimension
                            width = (height * ratio).toInt()
                        }
                    }

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } finally {
                    page?.close()
                }
                renderer.close()
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
