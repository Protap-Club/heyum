package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.pgstorm148.heyum.model.MealType
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MealSelectorScreen : Screen, KoinComponent {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mealLogRepository: MealLogRepository by inject()

        val todayLogs by mealLogRepository.getTodayLogs().collectAsState(initial = emptyList())
        val completedMeals = todayLogs.map { it.mealType }.distinct()
        val allMeals = listOf("BREAKFAST", "LUNCH", "SNACKS", "DINNER")
        val missingMeals = allMeals.filter { it !in completedMeals }

        val currentHour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        val currentMealType = when {
            currentHour in 6..10 -> MealType.BREAKFAST
            currentHour in 11..14 -> MealType.LUNCH
            currentHour in 15..18 -> MealType.SNACKS
            currentHour in 19..23 -> MealType.DINNER
            else -> MealType.LATE_NIGHT
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("heYUM.co", color = Color(0xFFF4F4F5), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("What are you planning? \uD83C\uDF7D\uFE0F", color = Color(0xFFF4F4F5), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Choose your meal and we'll find the perfect dish", color = Color(0xFFA1A1AA), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            val mealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACKS, MealType.DINNER, MealType.LATE_NIGHT)
            
            mealTypes.forEach { mealType ->
                val isCurrent = mealType == currentMealType
                val loggedDish = todayLogs.firstOrNull { it.mealType == mealType.name }
                
                MealCard(
                    mealType = mealType,
                    isCurrent = isCurrent,
                    loggedDishName = loggedDish?.dishName,
                    onClick = {
                        // TODO: Make DecisionCardScreen take mealType parameter
                        // navigator.push(DecisionCardScreen(mealType = mealType.name))
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (missingMeals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                CompleteYourDayCard(
                    missingMeal = missingMeals.first(),
                    completedCount = completedMeals.size,
                    totalCount = allMeals.size,
                    onClick = {
                        val mealType = MealType.valueOf(missingMeals.first())
                        // TODO: navigator.push(DecisionCardScreen(mealType = mealType.name))
                    }
                )
            }
        }
    }

    @Composable
    fun MealCard(mealType: MealType, isCurrent: Boolean, loggedDishName: String?, onClick: () -> Unit) {
        val borderColor = if (isCurrent) Color(0xFFCCFF00) else Color(0xFF27272A)
        val borderStroke = if (isCurrent) BorderStroke(2.dp, borderColor) else BorderStroke(1.dp, borderColor)

        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            color = Color(0xFF18181B),
            shape = RoundedCornerShape(16.dp),
            border = borderStroke
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mealType.emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(mealType.displayName, color = Color(0xFFF4F4F5), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mealType.timeRange, color = Color(0xFF52525B), fontSize = 13.sp)
                    }
                    if (loggedDishName != null) {
                        Text("✅ $loggedDishName", color = Color(0xFFCCFF00), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Text("Tap to find your meal", color = Color(0xFFA1A1AA), fontSize = 14.sp)
                    }
                }
            }
        }
    }

    @Composable
    fun CompleteYourDayCard(missingMeal: String, completedCount: Int, totalCount: Int, onClick: () -> Unit) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF18181B),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(Color(0xFFCCFF00)))
                Column(modifier = Modifier.padding(20.dp)) {
                    val displayName = MealType.valueOf(missingMeal).displayName
                    Text("\uD83C\uDF19 Still need $displayName?", color = Color(0xFFF4F4F5), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(totalCount) { index ->
                            val color = if (index < completedCount) Color(0xFFCCFF00) else Color(0xFF52525B)
                            Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(6.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You've logged $completedCount of $totalCount meals today. Finish strong! \uD83D\uDCAA", color = Color(0xFFA1A1AA), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A5F))
                    ) {
                        Text("Find $displayName →", color = Color.White)
                    }
                }
            }
        }
    }
}
