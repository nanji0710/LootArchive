package com.nanji.lootarchive.util

import java.text.NumberFormat
import java.util.*

object CurrencyUtil {

    data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val name: String
    )

    val SUPPORTED_CURRENCIES = listOf(
        CurrencyInfo("CNY", "¥", "人民币"),
        CurrencyInfo("USD", "$", "美元"),
        CurrencyInfo("EUR", "€", "欧元"),
        CurrencyInfo("JPY", "¥", "日元"),
        CurrencyInfo("GBP", "£", "英镑"),
        CurrencyInfo("KRW", "₩", "韩元"),
        CurrencyInfo("HKD", "HK$", "港币")
    )

    fun getSymbol(code: String): String {
        return SUPPORTED_CURRENCIES.find { it.code == code }?.symbol ?: code
    }

    fun getName(code: String): String {
        return SUPPORTED_CURRENCIES.find { it.code == code }?.name ?: code
    }

    fun formatPrice(price: Double, currencyCode: String = "CNY"): String {
        val symbol = getSymbol(currencyCode)
        val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(price)
        return "$symbol$formatted"
    }
}
