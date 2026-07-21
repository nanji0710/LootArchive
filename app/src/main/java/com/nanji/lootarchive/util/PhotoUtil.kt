package com.nanji.lootarchive.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PhotoUtil {

    private const val PHOTO_DIR = "LootArchive/photos"
    private const val MAX_PHOTO_WIDTH = 1280
    private const val MAX_PHOTO_HEIGHT = 1280
    private const val JPEG_QUALITY = 70

    /**
     * 获取照片存储目录
     */
    fun getPhotoDir(context: Context): File {
        val dir = File(context.filesDir, PHOTO_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 生成唯一照片文件名
     */
    fun generatePhotoFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        return "IMG_$timestamp.jpg"
    }

    /**
     * 保存照片到本地并返回文件路径
     */
    fun savePhoto(context: Context, bitmap: Bitmap): String {
        val dir = getPhotoDir(context)
        val fileName = generatePhotoFileName()
        val file = File(dir, fileName)

        // 压缩并保存
        val scaledBitmap = scaleBitmap(bitmap, MAX_PHOTO_WIDTH, MAX_PHOTO_HEIGHT)
        FileOutputStream(file).use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        }
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        return file.absolutePath
    }

    /**
     * 从 Uri 保存照片
     */
    fun savePhotoFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap ?: return null

            val path = savePhoto(context, bitmap)
            bitmap.recycle()
            path
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 缩放 Bitmap
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 删除照片文件
     */
    fun deletePhoto(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取照片 Bitmap
     */
    fun loadPhoto(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }
}
