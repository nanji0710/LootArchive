package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextPrimary
import com.nanji.lootarchive.ui.theme.TextSecondary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import java.util.Calendar

@Composable
fun WheelDatePickerDialog(
    title: String,
    initialDateMillis: Long?,
    maxDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val cal = remember { Calendar.getInstance() }

    // 初始日期
    val initCal = remember(initialDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = initialDateMillis ?: System.currentTimeMillis()
        }
    }
    val maxCal = remember(maxDateMillis) {
        if (maxDateMillis != null) Calendar.getInstance().apply { timeInMillis = maxDateMillis }
        else null
    }

    var selectedYear by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH) + 1) } // 1-12
    var selectedDay by remember { mutableIntStateOf(initCal.get(Calendar.DAY_OF_MONTH)) }

    val today = Calendar.getInstance()
    val minYear = 2000
    val maxYear = maxCal?.get(Calendar.YEAR) ?: (today.get(Calendar.YEAR) + 20)

    val years = (minYear..maxYear).toList()
    val months = (1..12).toList()
    val daysInMonth = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply { set(selectedYear, selectedMonth - 1, 1) }
            .getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val maxDay = if (maxCal != null && selectedYear == maxCal.get(Calendar.YEAR) && selectedMonth == maxCal.get(Calendar.MONTH) + 1) {
        maxCal.get(Calendar.DAY_OF_MONTH)
    } else {
        daysInMonth
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFCFAF6),
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary(), modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 年月日列标题
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("年", "月", "日").forEach { label ->
                        Text(label, fontSize = 13.sp, color = TextAuxiliary(), modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                    }
                }

                // 选中日期预览
                Text(
                    "${selectedYear}年${selectedMonth}月${selectedDay}日",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary(),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // 三列滚动选择器
                Row(
                    Modifier.fillMaxWidth().height(180.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 年
                    WheelColumn(
                        items = years,
                        selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                        label = { "${it}年" },
                        modifier = Modifier.width(72.dp),
                        onSelected = { selectedYear = it }
                    )
                    // 月
                    WheelColumn(
                        items = months,
                        selectedIndex = months.indexOf(selectedMonth).coerceAtLeast(0),
                        label = { "${it}月" },
                        modifier = Modifier.width(72.dp),
                        onSelected = { selectedMonth = it }
                    )
                    // 日
                    val days = (1..maxDay).toList()
                    WheelColumn(
                        items = days,
                        selectedIndex = days.indexOf(selectedDay.coerceAtMost(maxDay)).coerceAtLeast(0),
                        label = { "${it}日" },
                        modifier = Modifier.width(72.dp),
                        onSelected = { selectedDay = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                cal.set(selectedYear, selectedMonth - 1, selectedDay)
                onConfirm(cal.timeInMillis)
            }) { Text("确定", color = Primary()) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary()) }
        }
    )
}

@Composable
private fun WheelColumn(
    items: List<Int>,
    selectedIndex: Int,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val idx = listState.firstVisibleItemIndex
        if (idx in items.indices) onSelected(items[idx])
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = index == listState.firstVisibleItemIndex

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isSelected) Modifier.background(Primary().copy(alpha = 0.15f))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label(item),
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Primary() else TextSecondary(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // 上下渐变遮罩
        Box(Modifier.fillMaxWidth().height(30.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(
                (if (isDark) Color(0xFF2A2A2A) else Color(0xFFFCFAF6)),
                Color.Transparent
            ))))
        Box(Modifier.fillMaxWidth().height(30.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(
                Color.Transparent,
                (if (isDark) Color(0xFF2A2A2A) else Color(0xFFFCFAF6))
            ))))
    }
}
