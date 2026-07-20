package com.nanji.lootarchive.domain.model

import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.local.entity.ItemPhotoEntity

/**
 * 聚合模型：物品 + 所属分类 + 照片列表
 */
data class ItemWithPhotos(
    val item: ItemEntity,
    val category: CategoryEntity?,
    val photos: List<ItemPhotoEntity>
) {
    val isWarrantyExpiring: Boolean
        get() {
            val expiry = item.warrantyExpiryDate ?: return false
            val now = System.currentTimeMillis()
            val sevenDays = 7 * 24 * 60 * 60 * 1000L
            return expiry > now && expiry <= now + sevenDays
        }

    val isWarrantyExpired: Boolean
        get() {
            val expiry = item.warrantyExpiryDate ?: return false
            return expiry < System.currentTimeMillis()
        }

    val warrantyStatusText: String
        get() = when {
            isWarrantyExpired -> "已过期"
            isWarrantyExpiring -> "即将到期"
            item.warrantyExpiryDate != null -> "保修中"
            else -> "无保修"
        }
}
