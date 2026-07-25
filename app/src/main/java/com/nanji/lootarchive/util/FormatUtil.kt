package com.nanji.lootarchive.util

import java.text.NumberFormat
import java.util.Locale

/**
 * 应用统一格式化工具
 */
object FormatUtil {

    /** 价格缩写格式化（≥1M 显示为 ¥x.xM，≥1万 显示为 ¥x.x万，≥1千 显示为 ¥x.xxK） */
    fun formatPriceShort(value: Double): String {
        return when {
            value >= 1_000_000 -> "¥${"%.1f".format(value / 1_000_000)}M"
            value >= 10_000 -> "¥${"%.1f".format(value / 10_000)}万"
            value >= 1_000 -> "¥${"%.2f".format(value / 1_000)}K"
            else -> "¥${value.toLong()}"
        }
    }

    /** 价格标准格式化（¥1,234,567） */
    fun formatPriceFull(value: Double, currencyCode: String = "CNY"): String {
        val symbol = when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "JPY" -> "¥"
            "GBP" -> "£"
            else -> "¥"
        }
        return "$symbol${NumberFormat.getNumberInstance(Locale.getDefault()).format(value)}"
    }

    /** 文件大小格式化 */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }
}
