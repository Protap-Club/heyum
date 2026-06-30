package com.pgstorm148.heyum.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.firebase.FirebaseAuth
import com.pgstorm148.heyum.firebase.UserRepository
import com.pgstorm148.heyum.model.*
import com.pgstorm148.heyum.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class OnboardingScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val auth: FirebaseAuth = koinInject()
        val userRepository: UserRepository = koinInject()
        val coroutineScope = rememberCoroutineScope()

        val pagerState = rememberPagerState(pageCount = { 3 })

        // Step 2 State: Cuisine selection
        val cuisinesList = listOf(
            CuisineItem(Cuisine.INDIAN, "Indian", "🍛"),
            CuisineItem(Cuisine.CHINESE, "Chinese", "🥢"),
            CuisineItem(Cuisine.ITALIAN, "Italian", "🍕"),
            CuisineItem(Cuisine.JAPANESE, "Japanese", "🍣"),
            CuisineItem(Cuisine.MEXICAN, "Mexican", "🌮"),
            CuisineItem(Cuisine.THAI, "Thai", "🍜"),
            CuisineItem(Cuisine.STREET_FOOD, "Street Food", "🍿"),
            CuisineItem(Cuisine.HEALTHY, "Healthy", "🥗"),
            CuisineItem(Cuisine.COMFORT, "Comfort", "🍲"),
            CuisineItem(Cuisine.FAST_FOOD, "Fast Food", "🍔")
        )
        val selectedCuisines = remember { mutableStateListOf<Cuisine>() }

        // Step 3 State: Essentials
        var selectedDiet by remember { mutableStateOf(DietaryRestriction.VEG) }
        var spiceTolerance by remember { mutableStateOf(3f) }
        var dislikesText by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        // Dark background Calm-app style container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)) // Dark charcoal background
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top logo or padding space
                Spacer(modifier = Modifier.height(16.dp))

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    userScrollEnabled = true
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (page) {
                                    0 -> {
                                        // --- Screen 1: Welcome ---
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "👋🍕",
                                            fontSize = 72.sp,
                                            modifier = Modifier.padding(bottom = 20.dp)
                                        )
                                        Text(
                                            text = "Tired of 'What do you want to eat?'",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextColor,
                                                fontSize = 24.sp,
                                                lineHeight = 32.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        Text(
                                            text = "We decide. You eat. Simple.",
                                            style = MaterialTheme.typography.bodyLarge.copy(color = TextMutedColor),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 36.dp)
                                        )
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(1)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                        ) {
                                            Text(
                                                "Let's go",
                                                style = MaterialTheme.typography.titleMedium.copy(color = SecondaryColor)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    1 -> {
                                        // --- Screen 2: Quick Taste Setup ---
                                        Text(
                                            text = "What do you love?",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextColor,
                                                fontSize = 22.sp
                                            ),
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // Grid of Cuisines in 3 columns
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp)
                                        ) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(3),
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(cuisinesList) { item ->
                                                    val isSelected = selectedCuisines.contains(item.cuisine)
                                                    CuisineCard(
                                                        item = item,
                                                        isSelected = isSelected,
                                                        onClick = {
                                                            if (isSelected) {
                                                                selectedCuisines.remove(item.cuisine)
                                                            } else {
                                                                selectedCuisines.add(item.cuisine)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(2)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                        ) {
                                            Text(
                                                "Next",
                                                style = MaterialTheme.typography.labelLarge.copy(color = SecondaryColor)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Skip",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextMutedColor,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier
                                                .clickable {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(2)
                                                    }
                                                }
                                                .padding(8.dp)
                                        )
                                    }

                                    2 -> {
                                        // --- Screen 3: The Essentials ---
                                        Text(
                                            text = "Almost there! Just a few things:",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextColor,
                                                fontSize = 20.sp
                                            ),
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // 1. Dietary Toggle chips
                                        Text(
                                            text = "Dietary preference",
                                            style = MaterialTheme.typography.labelLarge.copy(color = TextColor),
                                            modifier = Modifier
                                                .align(Alignment.Start)
                                                .padding(bottom = 8.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val diets = listOf(
                                                DietItem(DietaryRestriction.VEG, "🥬 Veg"),
                                                DietItem(DietaryRestriction.NON_VEG, "🍖 Non-Veg"),
                                                DietItem(DietaryRestriction.VEGAN, "🌱 Vegan"),
                                                DietItem(DietaryRestriction.JAIN, "🥣 Jain")
                                            )
                                            diets.forEach { item ->
                                                val isSelected = item.diet == selectedDiet
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isSelected) PrimaryColor else Color(0xFFF5F5F5))
                                                        .clickable { selectedDiet = item.diet }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = item.label,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = if (isSelected) SecondaryColor else TextColor,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // 2. Spice Meter
                                        val spiceEmoji = when (spiceTolerance.toInt()) {
                                            1 -> "😊 Mild"
                                            2 -> "🙂 Warm"
                                            3 -> "🌶️ Medium"
                                            4 -> "🌶️🌶️ Hot"
                                            else -> "🥵 Fire"
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Spice tolerance",
                                                style = MaterialTheme.typography.labelLarge.copy(color = TextColor)
                                            )
                                            Text(
                                                text = spiceEmoji,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = PrimaryColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Slider(
                                            value = spiceTolerance,
                                            onValueChange = { spiceTolerance = it },
                                            valueRange = 1f..5f,
                                            steps = 3,
                                            colors = SliderDefaults.colors(
                                                thumbColor = PrimaryColor,
                                                activeTrackColor = PrimaryColor,
                                                inactiveTrackColor = LightGray
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 3. Allergies/Dislikes Text Input
                                        Text(
                                            text = "Allergies / Dislikes (Optional)",
                                            style = MaterialTheme.typography.labelLarge.copy(color = TextColor),
                                            modifier = Modifier
                                                .align(Alignment.Start)
                                                .padding(bottom = 6.dp)
                                        )
                                        OutlinedTextField(
                                            value = dislikesText,
                                            onValueChange = { dislikesText = it },
                                            placeholder = {
                                                Text(
                                                    "e.g. peanuts, onion, mushrooms",
                                                    color = TextMutedColor.copy(alpha = 0.6f)
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryColor,
                                                unfocusedBorderColor = LightGray,
                                                cursorColor = PrimaryColor
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        if (isSaving) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = PrimaryColor)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    isSaving = true
                                                    coroutineScope.launch {
                                                        try {
                                                            val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
                                                            val dislikesList = dislikesText.split(",")
                                                                .map { it.trim() }
                                                                .filter { it.isNotEmpty() }

                                                            val profile = UserProfile(
                                                                id = userId,
                                                                name = "Gourmet Nom",
                                                                dietaryRestrictions = listOf(selectedDiet),
                                                                spiceTolerance = spiceTolerance.toInt(),
                                                                favoriteCuisines = selectedCuisines.toList(),
                                                                dislikedIngredients = dislikesList,
                                                                onboardingCompleted = true
                                                            )
                                                            
                                                            // Launch save in a background job so it doesn't block screen navigation
                                                            launch {
                                                                try {
                                                                    userRepository.saveUserProfile(profile)
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }

                                                            // Navigate immediately to DecisionCardScreen with showTooltip = true
                                                            navigator.replace(DecisionCardScreen(showTooltip = true))
                                                        } catch (e: Exception) {
                                                            // Handle save error
                                                        } finally {
                                                            isSaving = false
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                            ) {
                                                Text(
                                                    "Done! Start eating 🎉",
                                                    style = MaterialTheme.typography.labelLarge.copy(color = SecondaryColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tappable Page Indicator Dots
                PagerIndicator(
                    pageCount = 3,
                    currentPage = pagerState.currentPage,
                    onPageSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
fun CuisineCard(
    item: CuisineItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isHeartVisible by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            isHeartVisible = true
            heartScale.animateTo(1.4f, spring(dampingRatio = 0.5f, stiffness = 300f))
            heartScale.animateTo(1.0f, spring(dampingRatio = 0.6f, stiffness = 200f))
        } else {
            heartScale.animateTo(0f, tween(150))
            isHeartVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PrimaryColor.copy(alpha = 0.15f) else Color(0xFFF5F5F5))
            .border(
                width = 2.dp,
                color = if (isSelected) PrimaryColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = item.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PrimaryColor else TextColor,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        if (isHeartVisible || heartScale.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = "❤️",
                    fontSize = 12.sp,
                    modifier = Modifier.scale(heartScale.value)
                )
            }
        }
    }
}

@Composable
fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) PrimaryColor else Color.Gray.copy(alpha = 0.5f),
                animationSpec = tween(300)
            )

            Box(
                modifier = Modifier
                    .size(height = 8.dp, width = width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .clickable { onPageSelected(index) }
            )
        }
    }
}

data class CuisineItem(val cuisine: Cuisine, val name: String, val emoji: String)
data class DietItem(val diet: DietaryRestriction, val label: String)
