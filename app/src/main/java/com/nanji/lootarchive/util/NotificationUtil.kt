package com.nanji.lootarchive.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nanji.lootarchive.MainActivity
import com.nanji.lootarchive.data.local.entity.ItemEntity
import java.text.SimpleDateFormat
import java.util.*

object NotificationUtil {

    const val CHANNEL_WARRANTY = "warranty_reminder"
    const val CHANNEL_BACKUP = "backup_reminder"
    const val NOTIFICATION_ID_WARRANTY = 1001
    const val NOTIFICATION_ID_BACKUP = 2001

    /**
     * 创建通知渠道
     */
    fun createNotificationChannels(context: Context) {
        val warrantyChannel = NotificationChannel(
            CHANNEL_WARRANTY,
            "保修提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "物品保修到期提醒通知"
        }

        val backupChannel = NotificationChannel(
            CHANNEL_BACKUP,
            "备份提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "定期备份提醒通知"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(warrantyChannel)
        manager.createNotificationChannel(backupChannel)
    }

    /**
     * 发送保修到期通知
     */
    fun sendWarrantyNotification(context: Context, items: List<ItemEntity>) {
        if (!hasNotificationPermission(context)) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val title = "保修即将到期"
        val content = when (items.size) {
            0 -> return
            1 -> {
                val item = items.first()
                val expiry = item.warrantyExpiryDate?.let { dateFormat.format(Date(it)) } ?: ""
                "${item.name} 保修期将于 $expiry 到期"
            }
            else -> "共 ${items.size} 件物品保修即将到期，点击查看详情"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WARRANTY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_WARRANTY, notification)
    }

    /**
     * 发送备份提醒通知
     */
    fun sendBackupReminderNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("备份提醒")
            .setContentText("建议备份数据，避免物品信息丢失")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_BACKUP, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
