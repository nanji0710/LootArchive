package com.nanji.lootarchive.util

import android.util.Log
import android.util.Xml
import com.nanji.lootarchive.data.local.entity.ItemEntity
import org.xmlpull.v1.XmlPullParser
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 纯 Android 原生 .xlsx 读写，不依赖 Apache POI / XmlBeans。
 * .xlsx = ZIP 包内多个 XML 文件，Android 自带 ZIP + XmlPullParser 即可完全处理。
 */
object ExcelUtil {

    private const val TAG = "ExcelUtil"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateFormats = arrayOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()),
        SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    )

    // ──────────────────────────────────────────
    //  导出
    // ──────────────────────────────────────────

    fun exportItemsToExcel(items: List<ItemEntity>, exportDir: File): File {
        Log.d(TAG, "导出 ${items.size} 件物品...")
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "LootArchive_export_$ts.xlsx")

        ZipOutputStream(FileOutputStream(file)).use { zip ->

            zipEntry(zip, "[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""")

            zipEntry(zip, "_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")

            zipEntry(zip, "xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""")

            zipEntry(zip, "xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="物品清单" sheetId="1" r:id="rId1"/></sheets>
</workbook>""")

            zipEntry(zip, "xl/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"/>""")

            // shared strings
            val ss = mutableListOf<String>()
            val headers = arrayOf("物品名称", "分类ID", "购入价格", "购入日期", "保修到期日", "存放位置", "物品描述")
            headers.forEach { ss.add(it) }
            // 表头占前 7 个索引，数据从索引 7 开始

            val rows = items.map { item ->
                val indices = IntArray(7)
                indices[0] = addString(ss, item.name)
                indices[1] = addString(ss, item.categoryId.toString())
                indices[2] = addString(ss, formatPrice(item.purchasePrice))
                indices[3] = addString(ss, item.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "")
                indices[4] = addString(ss, item.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: "")
                indices[5] = addString(ss, item.storageLocation)
                indices[6] = addString(ss, item.description)
                indices
            }

            zipEntry(zip, "xl/sharedStrings.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${ss.size}" uniqueCount="${ss.size}">""")
                for (s in ss) append("<si><t>${esc(s)}</t></si>")
                append("</sst>")
            })

            zipEntry(zip, "xl/worksheets/sheet1.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><cols>""")
                val widths = intArrayOf(20, 8, 14, 14, 14, 16, 40)
                for ((i, w) in widths.withIndex())
                    append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
                append("</cols><sheetData>")

                // 表头 row=1
                append("""<row r="1">""")
                for ((ci, _) in headers.withIndex())
                    append("""<c r="${col(ci)}1" t="s"><v>$ci</v></c>""")
                append("</row>")

                // 数据行
                for ((ri, row) in rows.withIndex()) {
                    val rn = ri + 2
                    append("""<row r="$rn">""")
                    for ((ci, si) in row.withIndex())
                        append("""<c r="${col(ci)}$rn" t="s"><v>$si</v></c>""")
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            })

            zip.finish()
        }

        Log.d(TAG, "导出成功: ${file.absolutePath} (${file.length()} bytes)")
        return file
    }

    // ──────────────────────────────────────────
    //  导入（读取自己导出的 .xlsx）
    // ──────────────────────────────────────────

    fun importItemsFromExcel(file: File): List<ItemEntity> {
        Log.d(TAG, "导入: ${file.name}")
        val sharedStrings = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()
        var sheetBytes: ByteArray? = null

        ZipInputStream(FileInputStream(file)).use { zip ->
            var entry: ZipEntry?
            while (zip.nextEntry.also { entry = it } != null) {
                val name = entry!!.name
                when {
                    name == "xl/sharedStrings.xml" -> parseSharedStrings(zip, sharedStrings)
                    name.startsWith("xl/worksheets/sheet") -> sheetBytes = zip.readBytes()
                }
                zip.closeEntry()
            }
        }

        // 必须等 shared strings 解析完再解析 sheet（ZIP 条目顺序不确定）
        sheetBytes?.let { parseSheet(it.inputStream(), sharedStrings, rows) }
        if (sheetBytes == null) throw Exception("Excel 文件中未找到工作表")

        if (rows.isEmpty()) throw Exception("未找到数据行")

        // 跳过表头（第一行），从第二行开始解析
        val items = mutableListOf<ItemEntity>()
        for ((i, row) in rows.withIndex()) {
            if (i == 0) continue // 跳过表头
            try {
                val item = ItemEntity(
                    name = row.getOrElse(0) { "" },
                    categoryId = row.getOrElse(1) { "" }.toLongOrNull() ?: 0L,
                    purchasePrice = parsePrice(row.getOrElse(2) { "" }),
                    purchaseDate = parseDateAny(row.getOrElse(3) { "" }),
                    warrantyExpiryDate = parseDateAny(row.getOrElse(4) { "" }),
                    storageLocation = row.getOrElse(5) { "" },
                    description = row.getOrElse(6) { "" }
                )
                if (item.name.isNotBlank()) items.add(item)
            } catch (e: Exception) {
                Log.w(TAG, "跳过第 ${i + 1} 行", e)
            }
        }

        Log.d(TAG, "导入成功: ${items.size} 件")
        return items
    }

    // ──────────────────────────────────────────
    //  内部工具
    // ──────────────────────────────────────────

    private fun zipEntry(zip: ZipOutputStream, name: String, xml: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(xml.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun addString(list: MutableList<String>, s: String): Int {
        val idx = list.size
        list.add(s)
        return idx
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun col(c: Int) = ('A' + c).toString()

    private fun formatPrice(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else value.toString()

    private fun parsePrice(s: String): Double {
        val cleaned = s.replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun parseDateAny(s: String): Long? {
        if (s.isBlank()) return null
        for (df in dateFormats) {
            try { return df.parse(s)?.time } catch (_: Exception) {}
        }
        // 尝试纯数字：毫秒时间戳 或 Excel 序列号（天数，基准 1899-12-30）
        s.toLongOrNull()?.let {
            return if (it > 1_000_000_000_000L) it
            else (it - 25569) * 86_400_000L
        }
        return null
    }

    private fun parseSharedStrings(input: InputStream, out: MutableList<String>) {
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var event = parser.eventType
        var text = ""
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> { if (parser.name == "t") text = "" }
                XmlPullParser.TEXT -> { if (!parser.isWhitespace) text = parser.text }
                XmlPullParser.END_TAG -> { if (parser.name == "t") out.add(text) }
            }
            event = parser.next()
        }
    }

    private fun parseSheet(input: InputStream, sharedStrings: List<String>, rows: MutableList<List<String>>) {
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var event = parser.eventType
        var cellType = ""  // "s" = shared string, else inline number
        var cellValue = ""
        var cellRef = ""   // 必须在 START_TAG 上读取，END_TAG 取不到属性
        val colValues = mutableMapOf<Int, String>()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            colValues.clear()
                            cellValue = ""
                            cellType = ""
                        }
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellRef = parser.getAttributeValue(null, "r") ?: ""  // ⚠️ 必须在 START_TAG 读
                            cellValue = ""
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (!parser.isWhitespace) cellValue = parser.text
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            val colIdx = parseCol(cellRef)
                            val v = if (cellType == "s") {
                                cellValue.toIntOrNull()?.let { sharedStrings.getOrElse(it) { cellValue } } ?: cellValue
                            } else cellValue
                            if (colIdx >= 0) colValues[colIdx] = v
                            cellRef = ""
                        }
                        "row" -> {
                            if (colValues.isNotEmpty()) {
                                val maxCol = colValues.keys.maxOrNull() ?: -1
                                rows.add((0..maxCol).map { colValues[it] ?: "" })
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
    }

    private fun parseCol(ref: String): Int {
        // "A1" -> 0, "B2" -> 1, "AA3" -> 26
        val letters = ref.takeWhile { it.isLetter() }
        var col = 0
        for (c in letters) col = col * 26 + (c.uppercaseChar() - 'A' + 1)
        return col - 1
    }
}
