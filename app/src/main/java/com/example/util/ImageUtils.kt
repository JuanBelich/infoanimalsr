package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Reads image from contentResolver using Uri, downsizes it to maxDimension (default 1024px),
     * and compresses it into a JPEG byte array with requested quality (80% default).
     */
    fun compressImageUri(
        context: Context,
        uri: Uri,
        quality: Int = 80,
        maxDimension: Int = 1024
    ): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // First decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            var inSampleSize = 1
            val (width, height) = options.outWidth to options.outHeight
            if (width > maxDimension || height > maxDimension) {
                val halfWidth = width / 2
                val halfHeight = height / 2
                while (halfWidth / inSampleSize >= maxDimension && halfHeight / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return null

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error compressing image from Uri: $uri", e)
            null
        }
    }
}
