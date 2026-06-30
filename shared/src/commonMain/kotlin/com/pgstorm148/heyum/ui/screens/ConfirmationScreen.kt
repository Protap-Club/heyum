package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class ConfirmationScreen(
    val mealLogId: String,
    val dishName: String,
    val mealTypeStr: String,
    val mealEmoji: String
) : Screen, KoinComponent {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mealLogRepository: MealLogRepository by inject()
        val scope = rememberCoroutineScope()
        
        var selectedRating by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎉", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("$dishName logged!", color = Color(0xFFF4F4F5), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("for $mealTypeStr $mealEmoji", color = Color(0xFFA1A1AA), fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(48.dp))
            Text("How was it?", color = Color(0xFFF4F4F5), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RatingButton(
                    emoji = "😍",
                    label = "Loved",
                    selectedColor = Color(0xFFCCFF00),
                    isSelected = selectedRating == "LOVED",
                    onClick = {
                        selectedRating = "LOVED"
                        scope.launch { mealLogRepository.updateRating(mealLogId, "LOVED") }
                    }
                )
                RatingButton(
                    emoji = "😐",
                    label = "Okay",
                    selectedColor = Color(0xFFA1A1AA),
                    isSelected = selectedRating == "OKAY",
                    onClick = {
                        selectedRating = "OKAY"
                        scope.launch { mealLogRepository.updateRating(mealLogId, "OKAY") }
                    }
                )
                RatingButton(
                    emoji = "👎",
                    label = "Not for me",
                    selectedColor = Color(0xFFFF2A5F),
                    isSelected = selectedRating == "NOT_FOR_ME",
                    onClick = {
                        selectedRating = "NOT_FOR_ME"
                        scope.launch { mealLogRepository.updateRating(mealLogId, "NOT_FOR_ME") }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = { navigator.popUntil { it is MealSelectorScreen } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A5F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Find Another Meal →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { navigator.push(CravingLogScreen()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF27272A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Craving Log", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun RatingButton(emoji: String, label: String, selectedColor: Color, isSelected: Boolean, onClick: () -> Unit) {
        val bgColor = if (isSelected) selectedColor else Color(0xFF18181B)
        val contentColor = if (isSelected) Color.Black else Color.White
        
        Surface(
            modifier = Modifier.width(90.dp).height(110.dp),
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            border = if (!isSelected) BorderStroke(1.dp, Color(0xFF27272A)) else null,
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
