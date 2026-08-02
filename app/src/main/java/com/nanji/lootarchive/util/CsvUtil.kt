package com.nanji.lootarchive.util

/**
 * RFC 4180 CSV 字段转义 + 公式注入防护。
 * - 含逗号/引号/换行的字段用双引号包裹，内部引号加倍
 * - 以 = + - @ 开头的文本加前缀单引号，防止 Excel/Sheets 将内容当公式执行
 */
fun csvEscape(field: String): String {
    val value = if (
        field.startsWith("=") || field.startsWith("+") ||
        field.startsWith("-") || field.startsWith("@")
    ) {
        "'$field"
    } else {
        field
    }
    return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}
