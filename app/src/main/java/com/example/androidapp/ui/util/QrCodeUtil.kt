package com.example.androidapp.ui.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Utility for generating QR code bitmaps from text content.
 */
object QrCodeUtil {

    /**
     * Generates a QR code [Bitmap] encoding the given [content].
     *
     * @param content The text to encode in the QR code.
     * @param size The width and height of the resulting bitmap in pixels.
     * @return A [Bitmap] containing the QR code, or `null` if generation fails.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(
                content, BarcodeFormat.QR_CODE, size, size, hints
            )
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
