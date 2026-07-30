package com.nanji.lootarchive.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.ExpService
import com.nanji.lootarchive.data.local.dao.AchievementDao
import com.nanji.lootarchive.data.local.dao.UserProfileDao
import com.nanji.lootarchive.data.local.entity.AchievementEntity
import com.nanji.lootarchive.data.local.entity.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val achievements: List<AchievementEntity> = emptyList(),
    val isLoading: Boolean = true,
    val unlockMessage: String? = null
)

@HiltViewModel
class MyLandingViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val achievementDao: AchievementDao,
    private val expService: ExpService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // 补种成就种子数据（对已创建数据库但未种子的老用户）
        viewModelScope.launch {
            try {
                if (achievementDao.getCount() == 0) {
                    achievementDao.insertAll(seedAchievements())
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            userProfileDao.getProfile().catch { e ->
                android.util.Log.e("MyLandingVM", "profile flow error", e)
                emit(null)
            }.collect { profile ->
                _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
            }
        }
        viewModelScope.launch {
            achievementDao.getAllAchievements().catch { e ->
                android.util.Log.e("MyLandingVM", "achievements flow error", e)
                emit(emptyList())
            }.collect { achievements ->
                _uiState.value = _uiState.value.copy(achievements = achievements)
            }
        }
        viewModelScope.launch {
            expService.unlockFlow.collect { message ->
                _uiState.value = _uiState.value.copy(unlockMessage = message)
            }
        }
    }

    fun clearUnlockMessage() {
        _uiState.value = _uiState.value.copy(unlockMessage = null)
    }

    companion object {
        fun seedAchievements(): List<AchievementEntity> = listOf(
            AchievementEntity(key = "items_5", title = "初级收藏", description = "收集5件物品", category = "collection", target = 5),
            AchievementEntity(key = "items_20", title = "中级收藏家", description = "收集20件物品", category = "collection", target = 20),
            AchievementEntity(key = "items_50", title = "高级收藏家", description = "收集50件物品", category = "collection", target = 50),
            AchievementEntity(key = "items_100", title = "百物之主", description = "收集100件物品", category = "collection", target = 100),
            AchievementEntity(key = "value_10000", title = "万元户", description = "总资产超过1万", category = "value", target = 10000),
            AchievementEntity(key = "value_100000", title = "小富翁", description = "总资产超过10万", category = "value", target = 100000),
            AchievementEntity(key = "value_500000", title = "财富自由", description = "总资产超过50万", category = "value", target = 500000),
            AchievementEntity(key = "photos_10", title = "随手拍", description = "拍摄10张照片", category = "photo", target = 10),
            AchievementEntity(key = "photos_50", title = "摄影师", description = "拍摄50张照片", category = "photo", target = 50),
            AchievementEntity(key = "desc_10", title = "细节控", description = "完善10件物品描述", category = "detail", target = 10),
            AchievementEntity(key = "desc_50", title = "文字家", description = "完善50件物品描述", category = "detail", target = 50),
            AchievementEntity(key = "streak_7", title = "坚持一周", description = "连续7天活跃", category = "streak", target = 7),
            AchievementEntity(key = "streak_30", title = "月常打卡", description = "连续30天活跃", category = "streak", target = 30)
        )
    }
}
