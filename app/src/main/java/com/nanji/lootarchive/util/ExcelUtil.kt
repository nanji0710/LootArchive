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

object ExcelUtil {

    private const val TAG = "ExcelUtil"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateFormats = arrayOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()),
        SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    )

    // ─── 导出 ───

    fun exportItemsToExcel(items: List<ItemEntity>, exportDir: File): File {
        Log.d(TAG, "导出 ${items.size} 件物品...")
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "LootArchive_export_$ts.xlsx")

        ZipOutputStream(FileOutputStream(file)).use { zip ->

            add(zip, "[Content_Types].xml",
"""<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""")

            add(zip, "_rels/.rels",
"""<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")

            add(zip, "xl/_rels/workbook.xml.rels",
"""<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""")

            add(zip, "xl/workbook.xml",
"""<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="物品清单" sheetId="1" r:id="rId1"/></sheets>
</workbook>""")

            add(zip, "xl/styles.xml",
"""<?xml version="1.0" encoding="UTF-8"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"/>""")

            // Build shared strings
            val ss = mutableListOf<String>()
            val headers = arrayOf("物品名称", "分类ID", "购入价格", "购入日期", "保修到期日", "存放位置", "物品描述")

            data class RowRefs(val refs: IntArray) // shared string indices per column

            // Add headers to shared strings (indices 0-6)
            headers.forEach { ss.add(it) }

            // Add item data to shared strings, remembering indices
            val dataRefs = items.map { item ->
                RowRefs(IntArray(7).also { a ->
                    a[0] = store(ss, item.name)
                    a[1] = store(ss, item.categoryId.toString())
                    a[2] = store(ss, formatPrice(item.purchasePrice))
                    a[3] = store(ss, item.purchaseDate?.let { dateFormat.format(Date(it)) } ?: "")
                    a[4] = store(ss, item.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: "")
                    a[5] = store(ss, item.storageLocation)
                    a[6] = store(ss, item.description)
                })
            }

            // totalRefs = each unique string counted once per cell it appears in
            val totalRefs = headers.size + dataRefs.size * headers.size

            add(zip, "xl/sharedStrings.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>""")
                append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="$totalRefs" uniqueCount="${ss.size}">""")
                for (s in ss) append("<si><t xml:space=\"preserve\">${esc(s)}</t></si>")
                append("</sst>")
            })

            add(zip, "xl/worksheets/sheet1.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>""")
                append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><cols>""")
                val widths = intArrayOf(20, 8, 14, 14, 14, 16, 40)
                for ((i, w) in widths.withIndex())
                    append("""<col min="${i+1}" max="${i+1}" width="$w" customWidth="1"/>""")
                append("</cols><sheetData>")
                // header row
                append("""<row r="1">""")
                for (ci in headers.indices)
                    append("""<c r="${cellRef(ci, 1)}" t="s"><v>$ci</v></c>""")
                append("</row>")
                // data rows
                for ((ri, row) in dataRefs.withIndex()) {
                    val rn = ri + 2
                    append("""<row r="$rn">""")
                    for ((ci, si) in row.refs.withIndex())
                        append("""<c r="${cellRef(ci, rn)}" t="s"><v>$si</v></c>""")
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            })

            zip.finish()
        }

        Log.d(TAG, "导出成功: ${file.absolutePath} (${file.length()} bytes)")
        return file
    }

    // ─── 导入 ───

    fun importItemsFromExcel(file: File): List<ItemEntity> {
        Log.d(TAG, "导入: ${file.name}")
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        ZipInputStream(FileInputStream(file)).use { zip ->
            var entry: ZipEntry?
            while (zip.nextEntry.also { entry = it } != null) {
                when (entry!!.name) {
                    "xl/sharedStrings.xml" -> parseSharedStrings(zip, sharedStrings)
                    "xl/worksheets/sheet1.xml" -> sheetBytes = zip.readBytes()
                }
                zip.closeEntry()
            }
        }

        val rows = mutableListOf<List<String>>()
        sheetBytes?.let { parseSheet(it.inputStream(), sharedStrings, rows) }
            ?: throw Exception("Excel 文件中未找到工作表")
        if (rows.isEmpty()) throw Exception("未找到数据行")

        val items = mutableListOf<ItemEntity>()
        for ((i, row) in rows.withIndex()) {
            if (i == 0) continue // skip header
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
                Log.w(TAG, "跳过第 ${i+1} 行", e)
            }
        }
        Log.d(TAG, "导入成功: ${items.size} 件")
        return items
    }

    // ─── helpers ───

    private fun add(zip: ZipOutputStream, name: String, xml: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(xml.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun store(list: MutableList<String>, s: String): Int {
        val i = list.size
        list.add(s)
        return i
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("\n", "&#10;")
        .replace("\r", "")

    private fun cellRef(col: Int, row: Int): String {
        var c = col
        val sb = StringBuilder()
        while (c >= 0) {
            sb.insert(0, ('A' + (c % 26)).toChar())
            c = c / 26 - 1
        }
        return "$sb$row"
    }

    private fun formatPrice(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else value.toString()

    private fun parsePrice(s: String): Double =
        s.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0

    private fun parseDateAny(s: String): Long? {
        if (s.isBlank()) return null
        for (df in dateFormats) {
            try { return df.parse(s)?.time } catch (_: Exception) {}
        }
        s.toLongOrNull()?.let {
            return if (it > 1_000_000_000_000L) it else (it - 25569) * 86_400_000L
        }
        return null
    }

    // ─── XML parsers ───

    private fun parseSharedStrings(input: InputStream, out: MutableList<String>) {
        val p = Xml.newPullParser()
        p.setInput(input, "UTF-8")
        var e = p.eventType
        var text = ""
        while (e != XmlPullParser.END_DOCUMENT) {
            when (e) {
                XmlPullParser.START_TAG -> if (p.name == "t") text = ""
                XmlPullParser.TEXT -> if (!p.isWhitespace) text = p.text
                XmlPullParser.END_TAG -> if (p.name == "t") out.add(text)
            }
            e = p.next()
        }
    }

    private fun parseSheet(input: InputStream, ss: List<String>, rows: MutableList<List<String>>) {
        val p = Xml.newPullParser()
        p.setInput(input, "UTF-8")
        var e = p.eventType
        var cellType = ""; var cellRef = ""
        var inT = false; var inV = false
        var text = StringBuilder()
        val cv = mutableMapOf<Int, String>()

        while (e != XmlPullParser.END_DOCUMENT) {
            when (e) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> { cv.clear(); cellType = "" }
                    "c" -> {
                        cellType = p.getAttributeValue(null, "t") ?: ""
                        cellRef = p.getAttributeValue(null, "r") ?: ""
                    }
                    "t" -> { text.clear(); inT = true }
                    "v" -> { text.clear(); inV = true }
                }
                XmlPullParser.TEXT -> {
                    if (inT || inV) text.append(p.text)
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "v" -> { inV = false }
                    "c" -> {
                        val ci = parseCol(cellRef)
                        if (ci >= 0) {
                            cv[ci] = when (cellType) {
                                "s" -> text.toString().toIntOrNull()
                                    ?.let { ss.getOrElse(it) { text.toString() } }
                                    ?: text.toString()
                                else -> text.toString()
                            }
                        }
                    }
                    "row" -> {
                        if (cv.isNotEmpty()) {
                            val max = cv.keys.maxOrNull() ?: -1
                            rows.add((0..max).map { cv[it] ?: "" })
                        }
                    }
                }
            }
            e = p.next()
        }
    }

    private fun parseCol(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }
        if (letters.isEmpty()) return -1
        var c = 0
        for (ch in letters) c = c * 26 + (ch.uppercaseChar() - 'A' + 1)
        return c - 1
    }
}
