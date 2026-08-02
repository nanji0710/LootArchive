package com.nanji.lootarchive.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 回收站自动清理 Worker：
 * 每天检查一次，删除 14 天前进入回收站的物品（含照片文件）
 */
@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val itemRepository: ItemRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14)
            val expiredItems = itemRepository.getDeletedItemsBefore(threshold)

            // 先删文件（IO 线程）再事务删记录，避免孤儿数据
            expiredItems.forEach { item ->
                itemRepository.hardDeleteItemWithPhotos(item.id)
            }

            if (expiredItems.isNotEmpty()) {
                Log.i("TrashCleanup", "已自动清理 ${expiredItems.size} 件过期物品")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("TrashCleanup", "自动清理失败", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "trash_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrashCleanupWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
