package com.nanji.lootarchive.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {

    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    fun formatFull(date: Date): String = fullFormat.format(date)

    fun formatDate(date: Date): String = dateFormat.format(date)

    fun formatMonth(date: Date): String = monthFormat.format(date)

    fun formatTimestamp(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatTimestampFull(timestamp: Long): String = fullFormat.format(Date(timestamp))

    /**
     * 计算保修到期剩余天数
     */
    fun getRemainingDays(expiryDate: Long): Int {
        val now = System.currentTimeMillis()
        val diff = expiryDate - now
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * 获取天数后的时间戳
     */
    fun daysFromNow(days: Int): Long {
        return System.currentTimeMillis() + days * 24 * 60 * 60 * 1000L
    }

    /**
     * 获取月份开始时间戳
     */
    fun getMonthStart(monthsAgo: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * 格式化剩余天数文本
     */
    fun formatRemainingDays(expiryDate: Long): String {
        val days = getRemainingDays(expiryDate)
        return when {
            days < 0 -> "已过期 ${-days} 天"
            days == 0 -> "今天到期"
            days == 1 -> "明天到期"
            else -> "剩余 $days 天"
        }
    }
}
