package com.friday.assistant

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SharedContent {
    fun acceptedUri(uri: Uri) = uri.scheme == "content" && !uri.authority.isNullOrBlank()
    suspend fun image(context: Context, uri: Uri): InputImage = withContext(Dispatchers.IO) {
        require(acceptedUri(uri)) { "Choose an image using Android's file picker or Share." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000) { "Image dimensions are unsupported. Use a smaller image." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: error("Could not decode image.")
        val orientation = runCatching { context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } }.getOrNull()
        val angle = when (orientation) { ExifInterface.ORIENTATION_ROTATE_90 -> 90; ExifInterface.ORIENTATION_ROTATE_180 -> 180; ExifInterface.ORIENTATION_ROTATE_270 -> 270; else -> 0 }
        InputImage.fromBitmap(bitmap, angle)
    }
}
