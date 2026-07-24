package com.nanji.lootarchive.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.local.entity.ItemPhotoEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtil {

    private const val TAG = "BackupUtil"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    data class ExportItem(
        val item: ItemEntity,
        val photos: List<ItemPhotoEntity>
    )

    data class ImportItem(
        val item: ItemEntity,
        val photoFiles: List<File>  // temporary extracted photo files
    )

    // ─── 一键导出 ───

    fun fullExport(
        context: Context,
        items: List<ItemEntity>,
        allPhotos: List<ItemPhotoEntity>,
        exportDir: File
    ): File {
        Log.d(TAG, "一键导出 ${items.size} 件物品...")

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "LootArchive_完整备份_$ts.zip")

        // Group photos by itemId
        val photosByItem = allPhotos.groupBy { it.itemId }

        val exportItems = items.map { item ->
            ExportItem(item, photosByItem[item.id] ?: emptyList())
        }

        ZipOutputStream(FileOutputStream(file)).use { zip ->

            // 1. Excel 文件（供电脑查看）
            zip.putNextEntry(ZipEntry("物品数据.xlsx"))
            val xlsxFile = File(context.cacheDir, "temp_export.xlsx")
            try {
                ExcelUtil.exportItemsToExcel(items, context.cacheDir).also {
                    it.inputStream().use { input -> input.copyTo(zip) }
                    it.delete()
                }
            } catch (_: Exception) {}
            zip.closeEntry()

            // 2. Photos
            var photoIndex = 0
            val manifestItems = JSONArray()

            for (ei in exportItems) {
                val photoNames = JSONArray()
                for (photo in ei.photos.sortedBy { it.sortOrder }) {
                    photoIndex++
                    val ext = photo.photoPath.substringAfterLast('.', "jpg")
                    val zipPhotoName = "photos/${"%03d".format(photoIndex)}.$ext"
                    try {
                        val srcFile = File(photo.photoPath)
                        if (srcFile.exists()) {
                            zip.putNextEntry(ZipEntry(zipPhotoName))
                            srcFile.inputStream().use { input -> input.copyTo(zip) }
                            zip.closeEntry()
                            photoNames.put(zipPhotoName)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "跳过损坏照片: ${photo.photoPath}", e)
                    }
                }

                val itemJson = JSONObject().apply {
                    put("name", ei.item.name)
                    put("categoryId", ei.item.categoryId)
                    put("purchasePrice", ei.item.purchasePrice)
                    put("storageLocation", ei.item.storageLocation)
                    ei.item.purchaseDate?.let { put("purchaseDate", it) }
                    ei.item.warrantyExpiryDate?.let { put("warrantyExpiryDate", it) }
                    ei.item.warrantyPeriodDays?.let { put("warrantyPeriodDays", it) }
                    put("description", ei.item.description)
                    put("photos", photoNames)
                }
                manifestItems.put(itemJson)
            }

            // 3. manifest.json
            val manifest = JSONObject().apply {
                put("version", 1)
                put("exportDate", dateFormat.format(Date()))
                put("appVersion", "2.8.0")
                put("itemCount", items.size)
                put("items", manifestItems)
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.finish()
        }

        Log.d(TAG, "导出成功: ${file.absolutePath} (${file.length()} bytes)")
        return file
    }

    // ─── 一键导入 ───

    fun fullImport(
        context: Context,
        uri: Uri
    ): List<ImportItem> {
        Log.d(TAG, "一键导入...")

        // Step 1: Extract manifest.json and photos from the ZIP
        var manifestJson: JSONObject? = null
        val photoMap = mutableMapOf<String, ByteArray>()  // zipPath → bytes

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry?
                while (zip.nextEntry.also { entry = it } != null) {
                    when {
                        entry!!.name == "manifest.json" -> {
                            manifestJson = JSONObject(zip.reader().readText())
                        }
                        entry!!.name.startsWith("photos/") && !entry!!.isDirectory -> {
                            photoMap[entry!!.name] = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: throw Exception("无法读取备份文件")

        val manifest = manifestJson ?: throw Exception("备份文件中未找到 manifest.json")
        val itemsArray = manifest.getJSONArray("items")
        if (itemsArray.length() == 0) throw Exception("备份文件中没有物品数据")

        val photoDir = PhotoUtil.getPhotoDir(context)

        val result = mutableListOf<ImportItem>()

        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)

            val item = ItemEntity(
                name = obj.getString("name"),
                categoryId = obj.optLong("categoryId", 0L),
                purchasePrice = obj.optDouble("purchasePrice", 0.0),
                storageLocation = obj.optString("storageLocation", ""),
                purchaseDate = if (obj.has("purchaseDate")) obj.optLong("purchaseDate") else null,
                warrantyExpiryDate = if (obj.has("warrantyExpiryDate")) obj.optLong("warrantyExpiryDate") else null,
                warrantyPeriodDays = if (obj.has("warrantyPeriodDays")) obj.optInt("warrantyPeriodDays").takeIf { it > 0 } else null,
                description = obj.optString("description", "")
            )

            // Extract photos
            val photos = obj.optJSONArray("photos") ?: JSONArray()
            val photoFiles = mutableListOf<File>()
            for (j in 0 until photos.length()) {
                val zipPath = photos.getString(j)
                val bytes = photoMap[zipPath] ?: continue
                val fileName = PhotoUtil.generatePhotoFileName()
                val destFile = File(photoDir, fileName)
                destFile.writeBytes(bytes)
                photoFiles.add(destFile)
            }

            result.add(ImportItem(item, photoFiles))
        }

        Log.d(TAG, "导入成功: ${result.size} 件物品")
        return result
    }
}
