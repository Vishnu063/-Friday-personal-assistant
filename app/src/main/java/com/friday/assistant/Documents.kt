package com.friday.assistant

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Documents {
    suspend fun read(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val type = context.contentResolver.getType(uri).orEmpty()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (output.size() <= 10 * 1024 * 1024) {
                val count = input.read(buffer, 0, minOf(buffer.size, 10 * 1024 * 1024 + 1 - output.size()))
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            val bytes = output.toByteArray()
            require(bytes.size <= 10 * 1024 * 1024) { "Choose a document smaller than 10 MB." }
            if (type == "application/pdf" || bytes.take(4).toByteArray().toString(Charsets.US_ASCII) == "%PDF") {
                PDFBoxResourceLoader.init(context)
                PDDocument.load(bytes).use { pdf -> PDFTextStripper().apply { endPage = minOf(pdf.numberOfPages, 15) }.getText(pdf).take(12000) }
            } else bytes.toString(Charsets.UTF_8).take(12000)
        }?.takeIf { it.isNotBlank() } ?: error("No readable text. For a scanned PDF, import a screenshot with OCR.")
    }
}
