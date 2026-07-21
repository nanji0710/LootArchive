package com.nanji.lootarchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
            LootArchiveTheme(themeMode = themeMode) {
                key("main") { MainScreen() }
            }
        }
    }
}
