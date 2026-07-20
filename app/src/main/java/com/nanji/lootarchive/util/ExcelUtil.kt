package com.nanji.lootarchive.util

import com.nanji.lootarchive.data.local.entity.ItemEntity
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 导出物品列表为 Excel 文件
     */
    fun exportItemsToExcel(items: List<ItemEntity>, exportDir: File): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("物品清单")

        // 表头样式
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply { bold = true })
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // 表头
        val headers = arrayOf("物品名称", "分类ID", "购入价格", "购入日期", "保修到期日", "存放位置", "物品描述")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
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

        // 自动调整列宽
        headers.indices.forEach { colIndex ->
            sheet.autoSizeColumn(colIndex)
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "LootArchive_export_$timestamp.xlsx")
        FileOutputStream(file).use { output ->
            workbook.write(output)
        }
        workbook.close()

        return file
    }

    /**
     * 从 Excel 文件导入物品数据
     */
    fun importItemsFromExcel(file: File): List<ItemEntity> {
        val items = mutableListOf<ItemEntity>()
        val workbook = WorkbookFactory.create(FileInputStream(file))
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
                // 跳过格式异常的行
                continue
            }
        }
        workbook.close()
        return items
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
