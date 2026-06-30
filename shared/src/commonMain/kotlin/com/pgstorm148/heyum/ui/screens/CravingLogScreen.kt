package com.pgstorm148.heyum.ui.screens

import kotlinx.datetime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.engine.MealLogRepository
import com.pgstorm148.heyum.model.MealLogEntry
import com.pgstorm148.heyum.model.MealType
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CravingLogScreen : Screen, KoinComponent {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mealLogRepository: MealLogRepository by inject()

        var selectedFilter by remember { mutableStateOf("All") }
        val filters = listOf("All", "Loved 😍", "This Week", "By Cuisine")

        val allLogs by mealLogRepository.getAllLogs().collectAsState(initial = emptyList())

        val filteredLogs = remember(selectedFilter, allLogs) {
            when (selectedFilter) {
                "Loved 😍" -> allLogs.filter { it.rating == "LOVED" }
                "This Week" -> {
                    val oneWeekAgo = Clock.System.now().toEpochMilliseconds() - 7L * 24 * 60 * 60 * 1000
                    allLogs.filter { it.timestamp >= oneWeekAgo }
                }
                "By Cuisine" -> allLogs.sortedBy { it.cuisine }
                else -> allLogs
            }
        }

        val groupedLogs = remember(filteredLogs) {
            val map = filteredLogs.groupBy { entry ->
                val instant = Instant.fromEpochMilliseconds(entry.timestamp)
                val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${dateTime.year}-${dateTime.monthNumber.toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')}"
            }
            // Sort by descending keys (newest dates first)
            map.toSortedMap(reverseOrder())
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CRAVING LOG", color = Color(0xFFF4F4F5), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
                )
            },
            containerColor = Color(0xFF0A0A0A)
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Filters
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = filter == selectedFilter
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFFCCFF00) else Color(0xFF18181B),
                            border = if (!isSelected) BorderStroke(1.dp, Color(0xFF27272A)) else null,
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (allLogs.isEmpty()) {
                    EmptyState(navigator)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedLogs.forEach { (dateString, entries) ->
                            item {
                                DateGroupHeader(dateString)
                            }
                            items(listOf("BREAKFAST", "LUNCH", "SNACKS", "DINNER", "LATE_NIGHT")) { mealTypeStr ->
                                val entry = entries.firstOrNull { it.mealType == mealTypeStr }
                                val mealType = MealType.valueOf(mealTypeStr)
                                MealLogItem(mealType = mealType, entry = entry)
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun EmptyState(navigator: cafe.adriel.voyager.navigator.Navigator) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📝", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No cravings logged yet!", color = Color(0xFFF4F4F5), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Start by picking a meal and tapping Yum on dishes you love.",
                color = Color(0xFFA1A1AA),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { navigator.popUntil { it is MealSelectorScreen } },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A5F))
            ) {
                Text("Find My First Meal →", color = Color.White)
            }
        }
    }

    @Composable
    fun DateGroupHeader(dateString: String) {
        Text(
            text = "📅 $dateString",
            color = Color(0xFFF4F4F5),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    @Composable
    fun MealLogItem(mealType: MealType, entry: MealLogEntry?) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            color = Color(0xFF18181B),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mealType.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(mealType.displayName, color = Color(0xFFA1A1AA), fontSize = 12.sp)
                    if (entry != null) {
                        Text(entry.dishName, color = Color(0xFFF4F4F5), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Text("— Not logged", color = Color(0xFF52525B), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (entry?.rating != null) {
                    val ratingEmoji = when (entry.rating) {
                        "LOVED" -> "😍"
                        "OKAY" -> "😐"
                        "NOT_FOR_ME" -> "👎"
                        else -> ""
                    }
                    Text(ratingEmoji, fontSize = 20.sp)
                }
            }
        }
    }
}
