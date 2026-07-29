package com.nanji.lootarchive.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.dao.AchievementDao
import com.nanji.lootarchive.data.local.dao.UserProfileDao
import com.nanji.lootarchive.data.local.entity.AchievementEntity
import com.nanji.lootarchive.data.local.entity.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val achievements: List<AchievementEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MyLandingViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val achievementDao: AchievementDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileDao.getProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
            }
        }
        viewModelScope.launch {
            achievementDao.getAllAchievements().collect { achievements ->
                _uiState.value = _uiState.value.copy(achievements = achievements)
            }
        }
    }
}
