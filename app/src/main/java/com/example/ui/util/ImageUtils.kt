package com.example.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import java.io.ByteArrayOutputStream

object ImageUtils {

    val availableFilters = listOf(
        "Normal",
        "Warm",
        "Cool",
        "Black & White",
        "HDR",
        "Blur Background"
    )

    fun colorFilterFor(filterName: String): ColorFilter? {
        return when (filterName) {
            "Warm" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 15f,
                        0f, 1.05f, 0f, 0f, 5f,
                        0f, 0f, 0.85f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Cool" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.88f, 0f, 0f, 0f, 0f,
                        0f, 1.02f, 0f, 0f, 5f,
                        0f, 0f, 1.2f, 0f, 18f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Black & White" -> ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0f) }
            )
            "HDR" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, -15f,
                        0f, 1.25f, 0f, 0f, -15f,
                        0f, 0f, 1.25f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            else -> null
        }
    }

    fun uriToCompressedBase64(context: Context, uri: Uri, maxDim: Int = 640): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return ""
            bitmapToCompressedBase64(original, maxDim)
        } catch (_: Exception) {
            ""
        }
    }

    fun bitmapToCompressedBase64(bitmap: Bitmap, maxDim: Int = 640): String {
        return try {
            val ratio = minOf(
                maxDim.toFloat() / bitmap.width.coerceAtLeast(1),
                maxDim.toFloat() / bitmap.height.coerceAtLeast(1),
                1f
            )
            val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun saveBase64ToGallery(context: Context, base64: String, title: String): Boolean {
        val bitmap = decodeBase64ToBitmap(base64) ?: return false
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "${title}_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MeraInsta")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
