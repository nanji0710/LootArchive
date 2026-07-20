package com.nanji.lootarchive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.component.GlassCard
import com.nanji.lootarchive.ui.theme.*

@Composable
fun MyLandingScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCategory: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 头像和信息区
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Primary().copy(alpha = 0.15f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, Modifier.size(36.dp), tint = Primary())
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("拾物集", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary())
                    Text("你的私人物品资产管理工具", fontSize = 13.sp, color = TextAuxiliary())
                }
            }
        }

        // 快捷入口卡片
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            MyMenuItem(Icons.Outlined.Settings, "设置", "主题模式、提醒、数据备份") { onNavigateToSettings() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = glassBorderColor())
            MyMenuItem(Icons.Outlined.Category, "分类管理", "管理物品分类") { onNavigateToCategory() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = glassBorderColor())
            MyMenuItem(Icons.Outlined.Backup, "备份与恢复", "导出Excel、备份数据") { onNavigateToBackup() }
        }

        // 关于
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("拾物集 ItemGlow", fontSize = 18.sp, color = TextPrimary())
            Spacer(Modifier.height(4.dp))
            Text("当前版本 v2.3.0", fontSize = 13.sp, color = TextAuxiliary())
        }
    }
}

@Composable
private fun MyMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = Primary())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary())
                Text(subtitle, fontSize = 13.sp, color = TextAuxiliary())
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextAuxiliary())
        }
    }
}
