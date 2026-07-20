package com.nanji.lootarchive.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.util.NotificationUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 保修到期提醒 Worker
 * 每天检查一次保修到期情况
 */
@HiltWorker
class WarrantyCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val reminderDays = settingsRepository.warrantyReminderDays.first()
            val threshold = System.currentTimeMillis() + reminderDays * 24 * 60 * 60 * 1000L
            val expiringItems = itemRepository.getWarrantyExpiringItems(threshold).first()

            if (expiringItems.isNotEmpty()) {
                NotificationUtil.sendWarrantyNotification(applicationContext, expiringItems)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<WarrantyCheckWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "warranty_check",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}

/**
 * 备份提醒 Worker
 * 每月检查是否需要备份
 */
@HiltWorker
class BackupReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val enabled = settingsRepository.backupReminderEnabled.first()
            if (enabled) {
                NotificationUtil.sendBackupReminderNotification(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupReminderWorker>(
                30, TimeUnit.DAYS
            )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "backup_reminder",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
