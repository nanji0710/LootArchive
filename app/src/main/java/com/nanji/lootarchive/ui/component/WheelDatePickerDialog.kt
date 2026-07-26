package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
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
    var selectedMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableIntStateOf(initCal.get(Calendar.DAY_OF_MONTH)) }

    val today = Calendar.getInstance()
    val maxYear = maxCal?.get(Calendar.YEAR) ?: (today.get(Calendar.YEAR) + 20)
    val years = (2000..maxYear).toList()
    val months = (1..12).toList()

    // 当前选中的年月对应的最大天数
    val maxDayOfMonth = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply { set(selectedYear, selectedMonth - 1, 1) }
            .getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    // 如果有 maxDate 且正好是同年同月，限制最大日期
    val effectiveMaxDay = if (maxCal != null && selectedYear == maxCal.get(Calendar.YEAR) && selectedMonth == maxCal.get(Calendar.MONTH) + 1) {
        maxCal.get(Calendar.DAY_OF_MONTH)
    } else {
        maxDayOfMonth
    }
    val days = (1..effectiveMaxDay).toList()

    // 修正 selectedDay 不超过有效范围
    LaunchedEffect(effectiveMaxDay) {
        if (selectedDay > effectiveMaxDay) selectedDay = effectiveMaxDay
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFCFAF6),
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary(), fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 选中预览
                Surface(
                    Modifier.padding(bottom = 12.dp),
                    RoundedCornerShape(10.dp),
                    color = Primary().copy(alpha = 0.12f)
                ) {
                    Text(
                        "${selectedYear}年${selectedMonth}月${selectedDay}日",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary(),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                // 列标题
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("年", fontSize = 12.sp, color = TextAuxiliary(), modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                    Text("月", fontSize = 12.sp, color = TextAuxiliary(), modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                    Text("日", fontSize = 12.sp, color = TextAuxiliary(), modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                }

                // 三列滚动选择器（点击选择）
                Row(
                    Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WheelColumn(
                        items = years,
                        selected = selectedYear,
                        format = { "${it}年" },
                        modifier = Modifier.width(72.dp),
                        onSelect = { selectedYear = it }
                    )
                    WheelColumn(
                        items = months,
                        selected = selectedMonth,
                        format = { "${it}月" },
                        modifier = Modifier.width(72.dp),
                        onSelect = { selectedMonth = it }
                    )
                    WheelColumn(
                        items = days,
                        selected = selectedDay,
                        format = { "${it}日" },
                        modifier = Modifier.width(72.dp),
                        onSelect = { selectedDay = it }
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
    selected: Int,
    format: (Int) -> String,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val itemHeight = 40

    // 初始滚动到选中项
    LaunchedEffect(items) {
        val idx = items.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem(maxOf(idx - 2, 0))
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 80.dp)  // 留出上下空间保证中间项可选择
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selected

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSelect(item)
                            scope.launch {
                                listState.animateScrollToItem(maxOf(index - 2, 0))
                            }
                        }
                        .then(
                            if (isSelected) Modifier.background(Primary().copy(alpha = 0.18f))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        format(item),
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Primary() else TextSecondary(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // 上下渐变遮罩
        val bg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFCFAF6)
        Box(Modifier.fillMaxWidth().height(30.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(bg, Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(30.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, bg))))
    }
}
