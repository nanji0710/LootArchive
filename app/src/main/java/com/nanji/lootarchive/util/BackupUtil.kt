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

    data class ExportCategory(
        val id: Long,
        val name: String,
        val sortOrder: Int,
        val iconName: String
    )

    data class ExportItem(
        val item: ItemEntity,
        val photos: List<ItemPhotoEntity>
    )

    data class ImportItem(
        val item: ItemEntity,
        val photoFiles: List<File>  // temporary extracted photo files
    )

    data class ImportResult(
        val items: List<ImportItem>,
        val categories: List<com.nanji.lootarchive.data.local.entity.CategoryEntity>
    )

    // ─── 一键导出 ───

    /** ZIP 内 Excel 明细的 entry 名（改这里要同步改导入侧的判断）。 */
    const val XLSX_ENTRY = "物品数据.xlsx"

    /** 备份文件名。默认落在 Download（下载）目录，名字保持可辨识。 */
    fun buildZipFileName(timestamp: String? = null): String {
        val ts = timestamp
            ?: SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "LootArchive_完整备份_$ts.zip"
    }

    /**
     * 把完整备份写成 ZIP 写到任意输出流 —— 供 Download 目录（MediaStore）与
     * 私有目录兜底两条路径复用，保证压缩包结构只有一份实现。
     * 内部会 close 掉 [out]（ZipOutputStream 需要收到关闭才会写完中央目录）。
     */
    fun writeZip(
        items: List<ItemEntity>,
        allPhotos: List<ItemPhotoEntity>,
        categories: List<com.nanji.lootarchive.data.local.entity.CategoryEntity>,
        tempDir: File,
        out: OutputStream
    ) {
        Log.d(TAG, "一键导出 ${items.size} 件物品...")

        // Group photos by itemId
        val photosByItem = allPhotos.groupBy { it.itemId }

        val exportItems = items.map { item ->
            ExportItem(item, photosByItem[item.id] ?: emptyList())
        }

        ZipOutputStream(out).use { zip ->

            // 1. Excel 文件（供电脑查看）
            // 这只是附加内容，恢复流程只依赖 manifest.json + photos/，
            // 所以生成失败就跳过它（记日志），别毁掉整包备份。
            // 注意以前这里吞掉异常后仍然 zip.closeEntry()，写出一个空的 xlsx，
            // 结果是"导出成功"但包里其实没有明细，比直接跳过更难排查。
            val xlsxFile = try {
                ExcelUtil.exportItemsToExcel(items, tempDir)
            } catch (e: Exception) {
                Log.w(TAG, "Excel 明细生成失败，跳过该 entry（不影响恢复）", e)
                null
            }
            if (xlsxFile != null) {
                try {
                    zip.putNextEntry(ZipEntry(XLSX_ENTRY))
                    xlsxFile.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                } finally {
                    xlsxFile.delete()
                }
            }

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
                    // 带上主键：恢复时按 id 认出"同一条记录"，从而覆盖更新而不是再插一条
                    put("id", ei.item.id)
                    put("name", ei.item.name)
                    put("categoryId", ei.item.categoryId)
                    put("purchasePrice", ei.item.purchasePrice)
                    put("storageLocation", ei.item.storageLocation)
                    ei.item.purchaseDate?.let { put("purchaseDate", it) }
                    ei.item.warrantyExpiryDate?.let { put("warrantyExpiryDate", it) }
                    ei.item.warrantyPeriodDays?.let { put("warrantyPeriodDays", it) }
                    put("description", ei.item.description)
                    put("status", ei.item.status)
                    ei.item.salePrice?.let { put("salePrice", it) }
                    ei.item.saleDate?.let { put("saleDate", it) }
                    put("tags", ei.item.tags)
                    ei.item.lastStatusChangedAt?.let { put("lastStatusChangedAt", it) }
                    put("photos", photoNames)
                }
                manifestItems.put(itemJson)
            }

            // 3. Categories (include IDs for import remapping)
            val manifestCategories = JSONArray()
            for (cat in categories) {
                manifestCategories.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("sortOrder", cat.sortOrder)
                    put("iconName", cat.iconName)
                })
            }

            // 4. manifest.json
            val manifest = JSONObject().apply {
                put("version", 4)
                put("exportDate", dateFormat.format(Date()))
                put("appVersion", com.nanji.lootarchive.BuildConfig.VERSION_NAME)
                put("itemCount", items.size)
                put("categoryCount", categories.size)
                put("categories", manifestCategories)
                put("items", manifestItems)
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    // ─── 一键导入 ───

    /** 备份记录里存的是 content:// URI 或绝对路径，这里统一解析。 */
    fun backupFileUri(filePath: String): Uri =
        if (filePath.startsWith("content://")) Uri.parse(filePath)
        else Uri.fromFile(File(filePath))

    /** 备份文件当前是否还能读到（记录可能是旧的，文件已被删除或用户清理过）。 */
    fun backupFileExists(context: Context, filePath: String): Boolean {
        if (filePath.startsWith("content://")) {
            return try {
                context.contentResolver.openInputStream(Uri.parse(filePath))?.use { true } ?: false
            } catch (e: Exception) {
                Log.w(TAG, "备份文件不可读: $filePath", e)
                false
            }
        }
        return File(filePath).let { it.exists() && it.length() > 0 }
    }

    fun fullImport(
        context: Context,
        uri: Uri
    ): ImportResult {
        Log.d(TAG, "一键导入...")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("无法读取备份文件")
        return inputStream.use { parseZip(context, it) }
    }

    /** 备份记录点击恢复时走这里：不需要用户再手动选文件。 */
    fun fullImportFromRecord(
        context: Context,
        filePath: String
    ): ImportResult {
        Log.d(TAG, "从备份记录恢复: $filePath")
        if (!backupFileExists(context, filePath)) throw Exception("备份文件不存在或已被删除")
        return fullImport(context, backupFileUri(filePath))
    }

    private fun parseZip(
        context: Context,
        inputStream: InputStream
    ): ImportResult {
        // Step 1: Extract manifest.json and photos from the ZIP
        var manifestJson: JSONObject? = null
        val photoMap = mutableMapOf<String, ByteArray>()  // zipPath → bytes

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

        val manifest = manifestJson ?: throw Exception("备份文件中未找到 manifest.json")
        val itemsArray = manifest.getJSONArray("items")
        if (itemsArray.length() == 0) throw Exception("备份文件中没有物品数据")

        // Parse categories (with IDs for remapping)
        val categoriesArray = manifest.optJSONArray("categories") ?: JSONArray()
        val categories = mutableListOf<com.nanji.lootarchive.data.local.entity.CategoryEntity>()
        for (i in 0 until categoriesArray.length()) {
            val catObj = categoriesArray.getJSONObject(i)
            categories.add(
                com.nanji.lootarchive.data.local.entity.CategoryEntity(
                    id = catObj.optLong("id", (i + 1).toLong()),
                    name = catObj.getString("name"),
                    sortOrder = catObj.optInt("sortOrder", i),
                    iconName = catObj.optString("iconName", "")
                )
            )
        }

        val photoDir = PhotoUtil.getPhotoDir(context)

        val result = mutableListOf<ImportItem>()

        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)

            val item = ItemEntity(
                // 旧版备份没有 id 字段，保持 0 → 走"按名称匹配"的兼容分支
                id = obj.optLong("id", 0L),
                name = obj.getString("name"),
                categoryId = obj.optLong("categoryId", 0L),
                purchasePrice = obj.optDouble("purchasePrice", 0.0),
                storageLocation = obj.optString("storageLocation", ""),
                purchaseDate = if (obj.has("purchaseDate")) obj.optLong("purchaseDate") else null,
                warrantyExpiryDate = if (obj.has("warrantyExpiryDate")) obj.optLong("warrantyExpiryDate") else null,
                warrantyPeriodDays = if (obj.has("warrantyPeriodDays")) obj.optInt("warrantyPeriodDays").takeIf { it > 0 } else null,
                description = obj.optString("description", ""),
                status = obj.optString("status", "active"),
                tags = obj.optString("tags", ""),
                lastStatusChangedAt = if (obj.has("lastStatusChangedAt")) obj.optLong("lastStatusChangedAt") else null,
                salePrice = if (obj.has("salePrice")) obj.optDouble("salePrice").takeIf { it > 0 } else null,
                saleDate = if (obj.has("saleDate")) obj.optLong("saleDate").takeIf { it > 0 } else null
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

        Log.d(TAG, "导入成功: ${result.size} 件物品, ${categories.size} 个分类")
        return ImportResult(items = result, categories = categories)
    }
}
