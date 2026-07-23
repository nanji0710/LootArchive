package com.nanji.lootarchive.util

import android.util.Log
import com.nanji.lootarchive.data.local.entity.ItemEntity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelUtil {

    private const val TAG = "ExcelUtil"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 导出物品列表为 Excel 文件
     *
     * 注意：必须在调用前确保当前线程的 contextClassLoader 已设置为 app 的 ClassLoader：
     *   Thread.currentThread().contextClassLoader = context.classLoader
     * XmlBeans/POI 在 Android 后台线程上初始化时需要它来加载内置 schema 资源。
     */
    fun exportItemsToExcel(items: List<ItemEntity>, exportDir: File): File {
        Log.d(TAG, "开始导出 ${items.size} 条物品...")

        try {
            val workbook = XSSFWorkbook()
            try {
                val sheet = workbook.createSheet("物品清单")

                val headerRow = sheet.createRow(0)
                val headers = arrayOf("物品名称", "分类ID", "购入价格", "购入日期", "保修到期日", "存放位置", "物品描述")
                headers.forEachIndexed { i, title -> headerRow.createCell(i).setCellValue(title) }

                items.forEachIndexed { rowIndex, item ->
                    val row = sheet.createRow(rowIndex + 1)
                    row.createCell(0).setCellValue(item.name)
                    row.createCell(1).setCellValue(item.categoryId.toString())
                    row.createCell(2).setCellValue(item.purchasePrice)
                    row.createCell(3).setCellValue(item.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "")
                    row.createCell(4).setCellValue(item.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: "")
                    row.createCell(5).setCellValue(item.storageLocation)
                    row.createCell(6).setCellValue(item.description)
                }

                val colWidths = intArrayOf(20, 8, 12, 14, 14, 16, 40)
                headers.indices.forEach { i ->
                    sheet.setColumnWidth(i, colWidths.getOrElse(i) { 12 } * 256)
                }

                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(exportDir, "LootArchive_export_$ts.xlsx")
                FileOutputStream(file).use { workbook.write(it) }
                Log.d(TAG, "导出成功: ${file.absolutePath}")
                return file
            } finally {
                workbook.close()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "导出失败", t)
            throw RuntimeException("Excel 导出失败: ${t.message}", t)
        }
    }

    fun importItemsFromExcel(file: File): List<ItemEntity> {
        Log.d(TAG, "开始导入: ${file.name}")
        val items = mutableListOf<ItemEntity>()
        try {
            val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(FileInputStream(file))
            try {
                val sheet = workbook.getSheetAt(0)
                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    try {
                        val item = ItemEntity(
                            name = row.getCell(0)?.stringCellValue ?: "",
                            categoryId = 0,
                            purchasePrice = row.getCell(2)?.numericCellValue ?: 0.0,
                            purchaseDate = parseDate(row.getCell(3)?.stringCellValue),
                            warrantyExpiryDate = parseDate(row.getCell(4)?.stringCellValue),
                            storageLocation = row.getCell(5)?.stringCellValue ?: "",
                            description = row.getCell(6)?.stringCellValue ?: ""
                        )
                        if (item.name.isNotBlank()) items.add(item)
                    } catch (_: Exception) { continue }
                }
                Log.d(TAG, "导入成功: ${items.size} 件")
            } finally { workbook.close() }
        } catch (t: Throwable) {
            Log.e(TAG, "导入失败", t)
            throw RuntimeException("Excel 导入失败: ${t.message}", t)
        }
        return items
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try { dateFormat.parse(dateStr)?.time } catch (_: Exception) { null }
    }
}
