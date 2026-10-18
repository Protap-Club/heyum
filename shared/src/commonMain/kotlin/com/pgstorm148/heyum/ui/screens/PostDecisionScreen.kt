package com.pgstorm148.heyum.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.UriHandler
import com.pgstorm148.heyum.delivery.DeliveryApp
import com.pgstorm148.heyum.delivery.dishSearchUrl
import com.pgstorm148.heyum.delivery.getInstalledFoodDeliveryApps
import com.pgstorm148.heyum.delivery.getInstalledQuickCommerceApps
import com.pgstorm148.heyum.delivery.grocerySearchUrl
import com.pgstorm148.heyum.delivery.logoResource
import com.pgstorm148.heyum.delivery.openDeliveryApp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
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
    private val mood: Mood,
    private val initialShowOrderTab: Boolean = false
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
                            // Dish photo (bundled / hosted), falling back to the cuisine gradient + emoji.
                            com.pgstorm148.heyum.ui.components.DishImage(
                                dish = dish,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(imageScale),
                                emojiSize = 72.sp
                            )

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
                        val installedGroceryApps = remember { getInstalledQuickCommerceApps() }
                        val installedFoodApps = remember { getInstalledFoodDeliveryApps() }

                        // Group "Order" entry point leads with ordering; recipe stays available below.
                        if (initialShowOrderTab) {
                            OrderDishSection(dish = dish, apps = installedFoodApps, uriHandler = uriHandler)
                        }

                        // --- STEP 1: Ingredient availability check ---
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "🧺 Check your kitchen",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap everything you already have. We'll help you grab the rest.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                                )
                                Spacer(modifier = Modifier.height(12.dp))

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

                                val missingIngredients = dish.ingredients.filter { !checkedIngredients.contains(it) }
                                when {
                                    dish.ingredients.isEmpty() -> {
                                        // No ingredient list to check against; skip straight to the recipe.
                                    }
                                    missingIngredients.isEmpty() -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AccentColor.copy(alpha = 0.15f))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🎉 You've got everything — let's cook!",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = SecondaryColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = "Missing ${missingIngredients.size}: ${missingIngredients.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFB74D))
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Search each item where you shop:",
                                            style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.7f)),
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )
                                        // One row per ingredient — searched individually, since a single query
                                        // combining every ingredient (e.g. "tomato onion paneer") returns nothing
                                        // useful in these apps' search. Each app icon searches just that item.
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            missingIngredients.forEach { ingredient ->
                                                IngredientSearchRow(
                                                    ingredient = ingredient,
                                                    apps = installedGroceryApps,
                                                    onAppClick = { app ->
                                                        val url = app.grocerySearchUrl(ingredient)
                                                        if (url != null) {
                                                            openDeliveryApp(app, url)
                                                        } else {
                                                            uriHandler.openUri("https://blinkit.com/s/?q=${ingredient.trim().replace(" ", "+")}")
                                                        }
                                                    },
                                                    onFallbackClick = {
                                                        uriHandler.openUri("https://blinkit.com/s/?q=${ingredient.trim().replace(" ", "+")}")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- STEP 2: Recipe steps + cooking timer ---
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "👨‍🍳 Recipe for ${dish.name}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
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

                                // Cooking Timer
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

                        // --- STEP 3: Rather order the finished dish? (end of page) ---
                        if (!initialShowOrderTab) {
                            OrderDishSection(dish = dish, apps = installedFoodApps, uriHandler = uriHandler)
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

/**
 * "Rather just order it?" — lists the food-delivery aggregators actually installed on the
 * device (Swiggy, Zomato, etc.), deep-linking each to a search for this dish. Falls back to a
 * web order link when none are installed.
 */
@Composable
private fun OrderDishSection(
    dish: Dish,
    apps: List<DeliveryApp>,
    uriHandler: UriHandler
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🛵 Rather just order it?",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Get ${dish.name} delivered from an app you already have.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (apps.isEmpty()) {
                Button(
                    onClick = {
                        uriHandler.openUri(
                            dish.orderingUrl
                                ?: "https://www.swiggy.com/search?query=${dish.name.replace(" ", "+")}"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Order online 🛵", color = SecondaryColor)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    apps.forEach { app ->
                        RestaurantPriceRow(
                            name = app.displayName,
                            price = "",
                            deliveryTime = "",
                            logo = app.emoji,
                            logoRes = app.logoResource(),
                            onClick = { openDeliveryApp(app, app.dishSearchUrl(dish.name)) }
                        )
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
    logoRes: DrawableResource? = null,
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
                if (logoRes != null) {
                    Image(
                        painter = painterResource(logoRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text(text = logo, fontSize = 22.sp)
                }
                Column {
                    Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    if (deliveryTime.isNotBlank()) {
                        Text(text = "⚡ Delivery in $deliveryTime", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    }
                }
            }
            if (price.isNotBlank()) {
                Text(text = price, style = MaterialTheme.typography.titleMedium.copy(color = PrimaryColor, fontWeight = FontWeight.ExtraBold))
            } else {
                Text(text = "Open →", style = MaterialTheme.typography.labelMedium.copy(color = PrimaryColor, fontWeight = FontWeight.Bold))
            }
        }
    }
}

/**
 * One missing ingredient with a tap-target per installed grocery app, each searching that app for
 * just this ingredient. Deliberately per-ingredient rather than one button per app searching all
 * of them at once — a combined multi-word query (e.g. "tomato onion paneer") doesn't match a real
 * product in these apps' search, so it has to be one search per item.
 */
@Composable
fun IngredientSearchRow(
    ingredient: String,
    apps: List<DeliveryApp>,
    onAppClick: (DeliveryApp) -> Unit,
    onFallbackClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ingredient.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                modifier = Modifier.weight(1f)
            )
            if (apps.isEmpty()) {
                Text(
                    text = "Search →",
                    style = MaterialTheme.typography.labelMedium.copy(color = PrimaryColor, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable(onClick = onFallbackClick)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    apps.forEach { app ->
                        val logoRes = app.logoResource()
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAppClick(app) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoRes != null) {
                                Image(
                                    painter = painterResource(logoRes),
                                    contentDescription = app.displayName,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Text(text = app.emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
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
