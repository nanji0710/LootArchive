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
     * 导出物品列表为 Excel 文件（最简版：无样式，避免 Android 上 AWT 相关闪退）
     */
    fun exportItemsToExcel(items: List<ItemEntity>, exportDir: File): File {
        Log.d(TAG, "开始导出 ${items.size} 条物品...")
        val workbook = XSSFWorkbook()
        try {
            val sheet = workbook.createSheet("物品清单")

            // 表头
            val headers = arrayOf("物品名称", "分类ID", "购入价格", "购入日期", "保修到期日", "存放位置", "物品描述")
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }

            // 数据行
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

            // 手动设置列宽
            val colWidths = intArrayOf(20, 8, 12, 14, 14, 16, 40)
            headers.indices.forEach { colIndex ->
                sheet.setColumnWidth(colIndex, colWidths.getOrElse(colIndex) { 12 } * 256)
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "LootArchive_export_$timestamp.xlsx")
            FileOutputStream(file).use { output ->
                workbook.write(output)
            }
            Log.d(TAG, "导出成功: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "导出失败", e)
            throw e
        } finally {
            workbook.close()
        }
    }

    /**
     * 从 Excel 文件导入物品数据
     */
    fun importItemsFromExcel(file: File): List<ItemEntity> {
        Log.d(TAG, "开始导入: ${file.name}")
        val items = mutableListOf<ItemEntity>()
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
                    if (item.name.isNotBlank()) {
                        items.add(item)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "跳过第 ${rowIndex + 1} 行", e)
                    continue
                }
            }
            Log.d(TAG, "导入成功: ${items.size} 件")
            return items
        } finally {
            workbook.close()
        }
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
