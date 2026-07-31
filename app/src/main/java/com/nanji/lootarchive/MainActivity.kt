package com.nanji.lootarchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.ui.MainScreen
import com.nanji.lootarchive.ui.onboarding.OnboardingScreen
import com.nanji.lootarchive.ui.theme.LootArchiveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "system")
            val primaryColor by settingsRepository.primaryColor.collectAsState(initial = 0xFFFFA500.toInt())
            // 用 first() 读取 DataStore 真实值（非 collectAsState 的 initial 默认值）
            // remember 初始为 false → 第一帧直接显示 MainScreen，读完 DataStore 后按需切换
            var showOnboarding by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                showOnboarding = !settingsRepository.onboardingCompleted.first()
            }
            LootArchiveTheme(themeMode = themeMode, primaryColor = primaryColor) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (showOnboarding) {
                        val scope = rememberCoroutineScope()
                        OnboardingScreen(
                            onComplete = {
                                scope.launch {
                                    settingsRepository.setOnboardingCompleted(true)
                                    showOnboarding = false
                                }
                            }
                        )
                    } else {
                        key("main") { MainScreen() }
                    }
                }
            }
        }
    }
}
