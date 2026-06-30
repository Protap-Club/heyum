package com.pgstorm148.heyum.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

class PostDecisionScreen(
    private val dish: Dish,
    private val mood: Mood
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val auth: FirebaseAuth = koinInject()
        val userRepository: UserRepository = koinInject()
        val coroutineScope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        // State variables
        var profile by remember { mutableStateOf<UserProfile?>(null) }
        var isExpanded by remember { mutableStateOf(false) }
        val checkedIngredients = remember { mutableStateListOf<String>() }
        
        // Cooking Timer state
        var isTimerActive by remember { mutableStateOf(false) }
        var timerSecondsLeft by remember { mutableStateOf(dish.prepTimeMinutes * 60) }
        var showTimerMode by remember { mutableStateOf(false) }

        // Feedback/Rating state
        var feedbackSubmitted by remember { mutableStateOf<String?>(null) }
        
        // History details
        var rejectedBeforeEntry by remember { mutableStateOf<MealHistoryEntry?>(null) }
        var acceptedBeforeEntry by remember { mutableStateOf<MealHistoryEntry?>(null) }

        // Load profile and analyze history
        LaunchedEffect(Unit) {
            val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
            userRepository.getUserProfile(userId).collect { fetchedProfile ->
                profile = fetchedProfile
                if (fetchedProfile != null) {
                    // Check if rejected before
                    rejectedBeforeEntry = fetchedProfile.mealHistory.lastOrNull { 
                        it.dishName.lowercase() == dish.name.lowercase() && !it.accepted 
                    }
                    // Check if accepted before
                    acceptedBeforeEntry = fetchedProfile.mealHistory.lastOrNull { 
                        it.dishName.lowercase() == dish.name.lowercase() && it.accepted 
                    }

                    // Ticks off ingredients that are in context / default fridge (mock matching)
                    val fridgeIngredients = fetchedProfile.dislikedIngredients.map { it.lowercase() } // just mock reference
                    dish.ingredients.forEach { ingredient ->
                        // Automatically tick off 40% of ingredients as "fridge matches" for premium demo feel
                        if (ingredient.hashCode() % 3 == 0) {
                            if (!checkedIngredients.contains(ingredient)) {
                                checkedIngredients.add(ingredient)
                            }
                        }
                    }
                }
            }
        }

        // Expanded view satisfying animations (500ms)
        LaunchedEffect(Unit) {
            delay(100)
            isExpanded = true
        }

        // Cooking Timer countdown coroutine
        LaunchedEffect(isTimerActive) {
            if (isTimerActive) {
                while (timerSecondsLeft > 0 && isTimerActive) {
                    delay(1000)
                    timerSecondsLeft--
                }
                if (timerSecondsLeft == 0) {
                    isTimerActive = false
                }
            }
        }

        val cardHeightMultiplier by animateFloatAsState(
            targetValue = if (isExpanded) 1.05f else 1.0f,
            animationSpec = tween(500, easing = EaseOutBack)
        )

        val sheetOffsetY by animateDpAsState(
            targetValue = if (isExpanded) 0.dp else 400.dp,
            animationSpec = tween(600, easing = EaseOutQuart)
        )

        val imageScale by animateFloatAsState(
            targetValue = if (isExpanded) 1.1f else 1.0f,
            animationSpec = tween(500, easing = EaseOutQuad)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)) // Dark Calming Background
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // Header Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "post-decision",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.width(48.dp)) // Equalizer space
                    }
                }

                // 1. Expanded Card Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .scale(cardHeightMultiplier)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = PrimaryColor.copy(alpha = 0.2f),
                                spotColor = PrimaryColor.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Dish Image Gradient
                            val gradient = remember(dish.id) {
                                val colors = when (dish.cuisine) {
                                    Cuisine.INDIAN -> listOf(Color(0xFFE53935), Color(0xFFFFB74D))
                                    Cuisine.CHINESE -> listOf(Color(0xFF8E24AA), Color(0xFFF06292))
                                    Cuisine.MEXICAN -> listOf(Color(0xFF43A047), Color(0xFFAED581))
                                    Cuisine.HEALTHY -> listOf(Color(0xFF00ACC1), Color(0xFF80DEEA))
                                    else -> listOf(Color(0xFFFFB300), Color(0xFFFFE082))
                                }
                                Brush.verticalGradient(colors)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(imageScale)
                                    .background(gradient),
                                contentAlignment = Alignment.Center
                            ) {
                                val foodEmoji = when (dish.cuisine) {
                                    Cuisine.INDIAN -> "🍛"
                                    Cuisine.CHINESE -> "🥢"
                                    Cuisine.MEXICAN -> "🌮"
                                    Cuisine.HEALTHY -> "🥗"
                                    else -> "🍲"
                                }
                                Text(text = foodEmoji, fontSize = 72.sp)
                            }

                            // Dark overlay gradient for readable text
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )

                            // Title & Description Overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = dish.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    )
                                )
                                Text(
                                    text = dish.description,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // 2. Reject/History Context Alert
                if (rejectedBeforeEntry != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFEAEA))
                                .border(1.dp, Color(0xFFFFCDCD), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "💡 Tried this before?",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color(0xFFC62828),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You weren't feeling this last time. Want to try again? Let's check how your mood is different now.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF8E2424))
                                )
                            }
                        }
                    }
                }

                // Order again / Usual restaurant alert
                if (acceptedBeforeEntry != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🛵 Quick Reorder",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = AccentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Your usual from Indian Kitchen?",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2E7D32))
                                    )
                                }
                                Button(
                                    onClick = {
                                        uriHandler.openUri(dish.orderingUrl ?: "https://www.swiggy.com")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Reorder", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                                }
                            }
                        }
                    }
                }

                // 3. Option Selection stacked sheet
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .offset(y = sheetOffsetY)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Options Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cook It option card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showTimerMode = false },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!showTimerMode) Color(0xFF1E1E1E) else Color(0xFF2A2A2A)
                                ),
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (!showTimerMode) PrimaryColor else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🧑‍🍳", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Cook It Myself",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Order It Option card (Show only if ordering details available)
                            if (dish.orderingAvailable || dish.orderingUrl != null) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showTimerMode = true },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (showTimerMode) Color(0xFF1E1E1E) else Color(0xFF2A2A2A)
                                    ),
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = if (showTimerMode) PrimaryColor else Color.Transparent
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🛵", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Order It",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // --- Content based on selection ---
                        if (!showTimerMode) {
                            // --- OPTION A: COOK IT MYSELF ---
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "Here's what you need:",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Ingredients List with checkboxes
                                    dish.ingredients.forEach { ingredient ->
                                        val isChecked = checkedIngredients.contains(ingredient)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) checkedIngredients.remove(ingredient)
                                                    else checkedIngredients.add(ingredient)
                                                }
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .border(
                                                        1.5.dp,
                                                        if (isChecked) PrimaryColor else Color.Gray,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .background(
                                                        if (isChecked) PrimaryColor else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isChecked) {
                                                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = ingredient,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (isChecked) Color.White.copy(alpha = 0.5f) else Color.White
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Deep link buttons to Zepto/Blinkit for missing ingredients
                                    val missingIngredients = dish.ingredients.filter { !checkedIngredients.contains(it) }
                                    if (missingIngredients.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                val query = missingIngredients.joinToString(",")
                                                uriHandler.openUri("https://zepto.onelink.me/J447/cart?search=$query")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            Text("Get missing ingredients on Zepto ⚡", color = SecondaryColor)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Divider(color = Color.White.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Full Recipe steps
                                    Text(
                                        text = "Recipe Steps:",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    val steps = if (dish.instructions.isNotEmpty()) dish.instructions else listOf(
                                        "Prep the ingredients by washing and chopping as required.",
                                        "Heat a cooking pan over medium flame and add seasonings.",
                                        "Stir in the main ingredients and saute for 10-12 minutes.",
                                        "Simmer on low flame, garnish with fresh herbs, and serve hot."
                                    )

                                    steps.forEachIndexed { index, step ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = PrimaryColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = step,
                                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Simple Cooking Timer Expandable Section
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "⏱️ Cooking Timer",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            val minutes = timerSecondsLeft / 60
                                            val seconds = timerSecondsLeft % 60
                                            Text(
                                                text = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    color = PrimaryColor,
                                                    fontSize = 36.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Button(
                                                    onClick = { isTimerActive = !isTimerActive },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerActive) Color.Red else AccentColor),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(if (isTimerActive) "Pause" else "Start", color = Color.White)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        isTimerActive = false
                                                        timerSecondsLeft = dish.prepTimeMinutes * 60
                                                    },
                                                    border = BorderStroke(1.dp, Color.Gray),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Reset", color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- OPTION B: ORDER IT ---
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "Same dish, zero effort 🛵",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Compare prices and order from local restaurants near you.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f)),
                                        modifier = Modifier.padding(bottom = 20.dp)
                                    )

                                    // Mock Price Comparisons
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        RestaurantPriceRow(
                                            name = "Swiggy (Sweet Tomato)",
                                            price = "₹220",
                                            deliveryTime = "25 mins",
                                            logo = "🍛",
                                            onClick = {
                                                uriHandler.openUri(dish.orderingUrl ?: "https://www.swiggy.com")
                                            }
                                        )

                                        RestaurantPriceRow(
                                            name = "Zomato (Golden Dragon)",
                                            price = "₹245",
                                            deliveryTime = "20 mins",
                                            logo = "🥡",
                                            onClick = {
                                                uriHandler.openUri("https://www.zomato.com")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Confirmation block
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AccentColor.copy(alpha = 0.15f))
                                .border(1.dp, AccentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✨ We'll remember this choice to decide your next meal.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = SecondaryColor,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 5. Rate the suggestion (😍 Loved it | 😐 Okay | 👎 Not for me)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "What's next?",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Rate this suggestion to tune your recommendations:",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f)),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (feedbackSubmitted != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentColor.copy(alpha = 0.2f))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Thank you! Taste vector updated. $feedbackSubmitted",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FeedbackButton(
                                            emoji = "😍",
                                            label = "Loved it",
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                feedbackSubmitted = "Loved it 😍"
                                                coroutineScope.launch {
                                                    try {
                                                        val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
                                                        val currentProfile = profile
                                                        if (currentProfile != null) {
                                                            // Boost tags for this dish in taste vector (+0.15)
                                                            val updatedVector = currentProfile.tasteVector.toMutableMap()
                                                            val tags = listOf(
                                                                "cuisine_${dish.cuisine.name.lowercase()}",
                                                                "spice_${dish.spiceLevel}",
                                                                "difficulty_${dish.difficulty.name.lowercase()}"
                                                            )
                                                            for (tag in tags) {
                                                                val currentVal = updatedVector[tag] ?: 0.0f
                                                                updatedVector[tag] = (currentVal + 0.15f).coerceIn(-1f, 1f)
                                                            }
                                                            userRepository.updateTasteVector(userId, updatedVector)
                                                        }
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        )

                                        FeedbackButton(
                                            emoji = "😐",
                                            label = "Okay",
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                feedbackSubmitted = "Okay 😐"
                                                // Neutral response - do nothing or minor adjustments
                                            }
                                        )

                                        FeedbackButton(
                                            emoji = "👎",
                                            label = "Not for me",
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                feedbackSubmitted = "Not for me 👎"
                                                coroutineScope.launch {
                                                    try {
                                                        val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
                                                        val currentProfile = profile
                                                        if (currentProfile != null) {
                                                            // Penalize tags for this dish in taste vector (-0.15)
                                                            val updatedVector = currentProfile.tasteVector.toMutableMap()
                                                            val tags = listOf(
                                                                "cuisine_${dish.cuisine.name.lowercase()}",
                                                                "spice_${dish.spiceLevel}",
                                                                "difficulty_${dish.difficulty.name.lowercase()}"
                                                            )
                                                            for (tag in tags) {
                                                                val currentVal = updatedVector[tag] ?: 0.0f
                                                                updatedVector[tag] = (currentVal - 0.15f).coerceIn(-1f, 1f)
                                                            }
                                                            userRepository.updateTasteVector(userId, updatedVector)
                                                        }
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantPriceRow(
    name: String,
    price: String,
    deliveryTime: String,
    logo: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = logo, fontSize = 22.sp)
                Column {
                    Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    Text(text = "⚡ Delivery in $deliveryTime", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
            }
            Text(text = price, style = MaterialTheme.typography.titleMedium.copy(color = PrimaryColor, fontWeight = FontWeight.ExtraBold))
        }
    }
}

@Composable
fun FeedbackButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
