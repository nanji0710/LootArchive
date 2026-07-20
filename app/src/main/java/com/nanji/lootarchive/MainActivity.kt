package com.nanji.lootarchive

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.repository.SettingsRepository
import com.nanji.lootarchive.ui.MainScreen
import com.nanji.lootarchive.ui.theme.LootArchiveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "system")
            val bgUri by settingsRepository.customBackgroundUri.collectAsState(initial = "")
            LootArchiveTheme(themeMode = themeMode) {
                Crossfade(themeMode, animationSpec = tween(1000)) {
                Box(Modifier.fillMaxSize()) {
                    // 全局自定义背景图
                    if (bgUri.isNotEmpty()) {
                        AsyncImage(model = Uri.parse(bgUri), contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        // 深色遮罩保证可读性
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    }
                    MainScreen()
                }
                } // Crossfade
            }
        }
    }
}
