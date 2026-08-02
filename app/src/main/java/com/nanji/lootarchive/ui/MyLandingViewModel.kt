package com.nanji.lootarchive.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.AchievementSeeds
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
            } catch (e: Exception) {
                android.util.Log.e("MyLandingVM", "Seed achievements failed", e)
            }
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
        fun seedAchievements(): List<AchievementEntity> = AchievementSeeds.all.map {
            AchievementEntity(key = it.key, title = it.title, description = it.description, category = it.category, target = it.target)
        }
    }
}
