package com.example.util

import android.graphics.Bitmap
import android.util.LruCache
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CodeGenerator {

    private val qrCache = LruCache<String, Bitmap>(30)
    private val barcodeCache = LruCache<String, Bitmap>(40)

    fun generateQrCodeBitmap(content: String, size: Int = 300): Bitmap? {
        if (content.isBlank()) return null
        val cacheKey = "$content-$size"
        qrCache.get(cacheKey)?.let { return it }

        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            qrCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateBarcodeBitmap(content: String, width: Int = 400, height: Int = 120): Bitmap? {
        if (content.isBlank()) return null
        val cacheKey = "$content-$width-$height"
        barcodeCache.get(cacheKey)?.let { return it }

        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.CODE_128,
                width,
                height
            )
            val w = bitMatrix.width
            val h = bitMatrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
            barcodeCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getQrCodeBitmapAsync(content: String, size: Int = 300): Bitmap? =
        withContext(Dispatchers.Default) {
            generateQrCodeBitmap(content, size)
        }

    suspend fun getBarcodeBitmapAsync(content: String, width: Int = 400, height: Int = 120): Bitmap? =
        withContext(Dispatchers.Default) {
            generateBarcodeBitmap(content, width, height)
        }
}

