package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PhotoCompressor {

    private const val TARGET_MAX_BYTES = 20 * 1024 // 20 KB
    private const val MAX_WIDTH = 380
    private const val MAX_HEIGHT = 480

    /**
     * Compresses an image from Uri to strictly around/under 20KB.
     */
    fun compressTo20Kb(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close() ?: return ByteArray(0)
        if (originalBitmap == null) return ByteArray(0)

        return compressBitmapTo20Kb(originalBitmap)
    }

    /**
     * Compresses an image from a local file path to strictly around/under 20KB.
     */
    fun compressFileTo20Kb(filePath: String): ByteArray {
        val file = File(filePath)
        if (!file.exists()) return ByteArray(0)
        val originalBitmap = BitmapFactory.decodeFile(filePath) ?: return ByteArray(0)
        return compressBitmapTo20Kb(originalBitmap)
    }

    /**
     * Resizes and iteratively compresses a Bitmap to reach <= 20KB JPEG.
     */
    fun compressBitmapTo20Kb(bitmap: Bitmap): ByteArray {
        var width = bitmap.width
        var height = bitmap.height

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            val ratio = width.toFloat() / height.toFloat()
            if (ratio > 1) {
                width = MAX_WIDTH
                height = (MAX_WIDTH / ratio).toInt()
            } else {
                height = MAX_HEIGHT
                width = (MAX_HEIGHT * ratio).toInt()
            }
        }
        var workingBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        var quality = 85
        var stream = ByteArrayOutputStream()
        workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        // Step-down quality loop
        while (stream.size() > TARGET_MAX_BYTES && quality > 15) {
            stream.reset()
            quality -= 10
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }

        // If still above 20KB, downscale dimensions slightly
        if (stream.size() > TARGET_MAX_BYTES) {
            val furtherScale = 0.8f
            val scaledDown = Bitmap.createScaledBitmap(
                workingBitmap,
                (workingBitmap.width * furtherScale).toInt(),
                (workingBitmap.height * furtherScale).toInt(),
                true
            )
            stream.reset()
            quality = 65
            scaledDown.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            while (stream.size() > TARGET_MAX_BYTES && quality > 15) {
                stream.reset()
                quality -= 10
                scaledDown.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }
        }

        return stream.toByteArray()
    }

    /**
     * Saves compressed 20KB bytes to app internal storage and returns the absolute path.
     */
    fun saveBytesToInternalStorage(context: Context, bytes: ByteArray, prefix: String = "student"): String {
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            out.write(bytes)
            out.flush()
        }
        return file.absolutePath
    }

    /**
     * Converts byte array to Base64 String for network transmission.
     */
    fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
