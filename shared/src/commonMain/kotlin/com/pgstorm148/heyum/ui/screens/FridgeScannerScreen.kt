package com.pgstorm148.heyum.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.engine.KitchenStorage
import com.pgstorm148.heyum.firebase.FirebaseAuth
import com.pgstorm148.heyum.firebase.DishRepository
import com.pgstorm148.heyum.model.*
import com.pgstorm148.heyum.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

sealed class KitchenStep {
    object InputScreen : KitchenStep()
    object MatchingLoading : KitchenStep()
    object ResultsScreen : KitchenStep()
}

class FridgeScannerScreen(val prepopulatedIngredient: String? = null) : Screen {

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        val auth: FirebaseAuth = koinInject()
        val dishRepository: DishRepository = koinInject()
        val kitchenStorage: KitchenStorage = koinInject()

        // Local state
        var currentStep by remember { mutableStateOf<KitchenStep>(KitchenStep.InputScreen) }
        val scannedIngredients = remember { mutableStateListOf<String>() }
        val recentIngredients = remember { mutableStateListOf<String>() }
        var newIngredientText by remember { mutableStateOf("") }
        var dishesPool by remember { mutableStateOf<List<Dish>>(emptyList()) }
        var matchingSuggestions by remember { mutableStateOf<List<Pair<Dish, Int>>>(emptyList()) }
        var youMightAlsoLikeDishes by remember { mutableStateOf<List<Pair<Dish, Int>>>(emptyList()) }
        
        var orderMissingItemsForDish by remember { mutableStateOf<Dish?>(null) }
        var orderMissingIngredients by remember { mutableStateOf<List<String>>(emptyList()) }

        // Local suggestions config
        val commonIngredients = remember {
            listOf(
                // Vegetables
                "Onion", "Tomato", "Potato", "Spinach", "Cauliflower", "Peas", "Carrot",
                "Bell Pepper", "Cucumber", "Mushroom", "Green Chili", "Coriander", "Lemon",
                // Proteins
                "Chicken", "Eggs", "Paneer", "Chickpeas", "Lentils", "Kidney Beans", "Fish",
                // Dairy
                "Milk", "Cheese", "Butter", "Cream", "Yogurt",
                // Grains & Bread
                "Rice", "Wheat Flour", "Bread", "Pasta", "Noodles", "Semolina",
                // Spices & Condiments
                "Garlic", "Ginger", "Cumin", "Turmeric", "Garam Masala", "Soy Sauce", "Olive Oil"
            )
        }

        // Categorized Suggested Ingredients
        val categories = remember {
            mapOf(
                "Vegetables 🥦" to listOf("Onion", "Tomato", "Potato", "Spinach", "Cauliflower", "Peas", "Carrot", "Bell Pepper", "Cucumber", "Mushroom", "Green Chili", "Coriander", "Lemon"),
                "Proteins 🍗" to listOf("Chicken", "Eggs", "Paneer", "Chickpeas", "Lentils", "Kidney Beans", "Fish"),
                "Dairy 🥛" to listOf("Milk", "Cheese", "Butter", "Cream", "Yogurt"),
                "Grains 🌾" to listOf("Rice", "Wheat Flour", "Bread", "Pasta", "Noodles", "Semolina"),
                "Spices 🌶️" to listOf("Garlic", "Ginger", "Cumin", "Turmeric", "Garam Masala", "Soy Sauce", "Olive Oil")
            )
        }

        var selectedCategory by remember { mutableStateOf("Vegetables 🥦") }

        // Fetch all dishes on startup to use for matching
        LaunchedEffect(Unit) {
            val allDishes = dishRepository.getDishesByContext(null, TimeOfDay.EVENING, "rainy").firstOrNull() ?: emptyList()
            dishesPool = allDishes

            // Load stored ingredients
            val saved = kitchenStorage.getIngredients()
            if (saved.isNotEmpty()) {
                scannedIngredients.clear()
                scannedIngredients.addAll(saved)
            }
            val recent = kitchenStorage.getRecentIngredients()
            recentIngredients.clear()
            recentIngredients.addAll(recent)

            if (prepopulatedIngredient != null) {
                if (!scannedIngredients.contains(prepopulatedIngredient)) {
                    scannedIngredients.add(prepopulatedIngredient)
                }
            }
        }

        // Trigger simulation matching loader
        LaunchedEffect(currentStep) {
            if (currentStep is KitchenStep.MatchingLoading) {
                delay(1500)
                
                // Match calculations
                val lowercasedOwned = scannedIngredients.map { it.lowercase().trim() }
                
                val allCalculated = dishesPool.map { dish ->
                    val dishIngredients = dish.ingredients.map { it.lowercase().trim() }
                    val matchPercent = if (dishIngredients.isEmpty()) 0 else {
                        val ownedCount = dishIngredients.count { lowercasedOwned.contains(it) }
                        ((ownedCount.toFloat() / dishIngredients.size) * 100).roundToInt()
                    }
                    dish to matchPercent
                }.sortedByDescending { it.second }

                // Main Suggestions: > 40% match
                val above40 = allCalculated.filter { it.second > 40 }
                val mainSuggestions = if (above40.size >= 5) {
                    above40
                } else {
                    // Backfill with top partial matches (including 0% match if needed to guarantee at least 5)
                    allCalculated.take(5)
                }

                // "You might also like" Suggestions: need exactly 1-2 more ingredients, excluding main suggestions
                val mainIds = mainSuggestions.map { it.first.id }.toSet()
                val likeSuggestions = dishesPool.filter { dish ->
                    val dishIngredients = dish.ingredients.map { it.lowercase().trim() }
                    val missingCount = dishIngredients.count { !lowercasedOwned.contains(it) }
                    missingCount in 1..2 && !mainIds.contains(dish.id)
                }.map { dish ->
                    val dishIngredients = dish.ingredients.map { it.lowercase().trim() }
                    val ownedCount = dishIngredients.count { lowercasedOwned.contains(it) }
                    val matchPercent = if (dishIngredients.isEmpty()) 0 else {
                        ((ownedCount.toFloat() / dishIngredients.size) * 100).roundToInt()
                    }
                    dish to matchPercent
                }.sortedByDescending { it.second }

                matchingSuggestions = mainSuggestions
                youMightAlsoLikeDishes = likeSuggestions
                currentStep = KitchenStep.ResultsScreen
            }
        }

        // Auto-suggestions logic based on text typed
        val autoSuggestMatches = remember(newIngredientText, scannedIngredients) {
            if (newIngredientText.isBlank()) emptyList()
            else {
                commonIngredients.filter {
                    it.contains(newIngredientText, ignoreCase = true) &&
                    !scannedIngredients.any { added -> added.equals(it, ignoreCase = true) }
                }.take(4)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SecondaryColor)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            when (currentStep) {
                KitchenStep.InputScreen -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { navigator.pop() },
                                modifier = Modifier.background(CardBackgroundColor, CircleShape)
                            ) {
                                Text("←", color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "What's in your kitchen?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        // Title Card
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "What ingredients do you have? 🥘",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = PrimaryColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "We'll find dishes you can make right now",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Last session recovery helper
                        val savedLastSession = remember { kitchenStorage.getIngredients() }
                        if (scannedIngredients.isEmpty() && savedLastSession.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardBackgroundColor)
                                    .border(1.dp, PrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        scannedIngredients.clear()
                                        scannedIngredients.addAll(savedLastSession)
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🔄 Use last session's ingredients", style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryColor, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        // Input field
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = newIngredientText,
                                    onValueChange = {
                                        if (it.endsWith(",") || it.endsWith("\n")) {
                                            val clean = it.replace(",", "").replace("\n", "").trim()
                                            if (clean.isNotBlank() && !scannedIngredients.any { ex -> ex.equals(clean, ignoreCase = true) }) {
                                                scannedIngredients.add(clean)
                                            }
                                            newIngredientText = ""
                                        } else {
                                            newIngredientText = it
                                        }
                                    },
                                    placeholder = { Text("Type an ingredient and press enter...", color = TextMutedColor.copy(alpha = 0.6f)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val clean = newIngredientText.trim()
                                        if (clean.isNotBlank() && !scannedIngredients.any { ex -> ex.equals(clean, ignoreCase = true) }) {
                                            scannedIngredients.add(clean)
                                        }
                                        newIngredientText = ""
                                    }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryColor,
                                        unfocusedBorderColor = LightGray,
                                        cursorColor = PrimaryColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Auto-suggestions list below input
                                if (autoSuggestMatches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Maybe:", style = MaterialTheme.typography.bodySmall.copy(color = TextMutedColor))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(autoSuggestMatches) { suggestion ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(LightGray.copy(alpha = 0.6f))
                                                        .clickable {
                                                            if (!scannedIngredients.contains(suggestion)) {
                                                                scannedIngredients.add(suggestion)
                                                            }
                                                            newIngredientText = ""
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(suggestion, fontSize = 12.sp, color = TextColor, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Added Ingredients Tags list
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 12.dp)
                        ) {
                            if (scannedIngredients.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No ingredients added yet",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor.copy(alpha = 0.7f))
                                    )
                                }
                            } else {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    scannedIngredients.forEach { ingredient ->
                                        // Bounce-in animation scale setup
                                        val chipScale = remember { Animatable(0f) }
                                        LaunchedEffect(ingredient) {
                                            chipScale.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .scale(chipScale.value)
                                                .clip(RoundedCornerShape(30.dp))
                                                .background(PrimaryColor.copy(alpha = 0.08f))
                                                .border(1.dp, PrimaryColor.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
                                                .clickable { scannedIngredients.remove(ingredient) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ingredient,
                                                color = TextColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "❌",
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SUGGESTED INGREDIENTS TABS
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Quick add suggestions:",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Category Selection Row
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                ) {
                                    items(categories.keys.toList()) { catName ->
                                        val isSelected = catName == selectedCategory
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) PrimaryColor else LightGray.copy(alpha = 0.4f))
                                                .clickable { selectedCategory = catName }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = catName,
                                                color = if (isSelected) SecondaryColor else TextColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Ingredients Horizontal scroll for Selected Category
                                val gridItems = categories[selectedCategory] ?: emptyList()
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    items(gridItems) { suggest ->
                                        val alreadyAdded = scannedIngredients.contains(suggest)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (alreadyAdded) LightGray.copy(alpha = 0.5f) else Color.White)
                                                .border(1.dp, if (alreadyAdded) Color.Transparent else LightGray, RoundedCornerShape(12.dp))
                                                .clickable(enabled = !alreadyAdded) {
                                                    scannedIngredients.add(suggest)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = if (alreadyAdded) "✓ $suggest" else "+ $suggest",
                                                color = if (alreadyAdded) TextMutedColor else TextColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // RECENT INGREDIENTS SECTION (Used last time)
                                if (recentIngredients.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LightGray.copy(alpha = 0.5f))
                                    Text(
                                        text = "Your recent items:",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMutedColor, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(recentIngredients) { recent ->
                                            val alreadyAdded = scannedIngredients.contains(recent)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (alreadyAdded) LightGray.copy(alpha = 0.5f) else Color.White)
                                                    .border(1.dp, if (alreadyAdded) Color.Transparent else LightGray, RoundedCornerShape(12.dp))
                                                    .clickable(enabled = !alreadyAdded) {
                                                        scannedIngredients.add(recent)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = recent,
                                                    fontSize = 12.sp,
                                                    color = if (alreadyAdded) TextMutedColor else TextColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions Panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val buttonEnabled = scannedIngredients.size >= 2
                            Button(
                                onClick = {
                                    if (buttonEnabled) {
                                        // Save settings
                                        kitchenStorage.saveIngredients(scannedIngredients.toList())
                                        val newRecent = (scannedIngredients + recentIngredients).distinct().take(10)
                                        kitchenStorage.saveRecentIngredients(newRecent)
                                        
                                        currentStep = KitchenStep.MatchingLoading
                                    }
                                },
                                enabled = buttonEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryColor,
                                    disabledContainerColor = LightGray
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .shadow(if (buttonEnabled) 4.dp else 0.dp, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = if (buttonEnabled) "Find Dishes (${scannedIngredients.size} ingredients added) 🍳" else "Add at least 2 ingredients",
                                    color = if (buttonEnabled) SecondaryColor else TextMutedColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            if (scannedIngredients.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = {
                                        scannedIngredients.clear()
                                        kitchenStorage.saveIngredients(emptyList())
                                    }
                                ) {
                                    Text("Clear All", color = PrimaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                KitchenStep.MatchingLoading -> {
                    // Loading matching view
                    Box(
                        modifier = Modifier.fillMaxSize().background(SecondaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing))
                            )

                            Text(
                                text = "🍳",
                                fontSize = 64.sp,
                                modifier = Modifier.rotate(angle)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Matching your ingredients... 🍳",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = PrimaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Checking for local recipe combinations",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
                            )
                        }
                    }
                }

                KitchenStep.ResultsScreen -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { currentStep = KitchenStep.InputScreen },
                                modifier = Modifier.background(CardBackgroundColor, CircleShape)
                            ) {
                                Text("←", color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "Yum Matches",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        // Expiring alerts banner if they have Paneer
                        val hasPaneer = scannedIngredients.any { it.lowercase().trim() == "paneer" }
                        if (hasPaneer) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFECE5))
                                    .border(1.dp, PrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        val palakPaneer = dishesPool.firstOrNull { it.id == "palak_paneer" }
                                        if (palakPaneer != null) {
                                            navigator.push(PostDecisionScreen(palakPaneer, Mood.COMFORT))
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("⏳", fontSize = 22.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Your paneer expires tomorrow! 🧀",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = PrimaryColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "Use it in Palak Paneer immediately.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextColor)
                                        )
                                    }
                                }
                            }
                        }

                        // Results List
                        if (matchingSuggestions.isEmpty()) {
                            // Empty State
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("😕", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No dish matches with just these ingredients 😕",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextColor,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try adding more ingredients or browse all dishes",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "💡 Suggestion:",
                                            style = MaterialTheme.typography.bodySmall.copy(color = PrimaryColor, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Popular combos: Tomato + Onion + Garlic = base for many dishes!",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextColor),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { navigator.pop() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Browse All", color = SecondaryColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Loaded List
                            Text(
                                text = "You can make:",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize().weight(1f)
                            ) {
                                items(matchingSuggestions) { (dish, percent) ->
                                    val dishIngredients = dish.ingredients
                                    val lowercasedOwned = scannedIngredients.map { it.lowercase().trim() }
                                    val missingIngredients = dishIngredients.filter {
                                        !lowercasedOwned.contains(it.lowercase().trim())
                                    }
                                    val ownedCount = dishIngredients.size - missingIngredients.size

                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(4.dp, RoundedCornerShape(20.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // Category Emoji Placeholder
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .background(PrimaryColor.copy(alpha = 0.1f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = when (dish.cuisine) {
                                                                Cuisine.INDIAN -> "🍛"
                                                                Cuisine.CHINESE -> "🥢"
                                                                Cuisine.ITALIAN -> "🍕"
                                                                Cuisine.MEXICAN -> "🌮"
                                                                Cuisine.JAPANESE -> "🍣"
                                                                Cuisine.THAI -> "🍜"
                                                                else -> "🍱"
                                                            },
                                                            fontSize = 22.sp
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = dish.name,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                color = TextColor,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Text(
                                                            text = dish.cuisine.name.lowercase().replaceFirstChar { it.uppercase() },
                                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMutedColor)
                                                        )
                                                    }
                                                }

                                                // Match Badge
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(30.dp))
                                                        .background(if (percent == 100) AccentColor.copy(alpha = 0.15f) else PrimaryColor.copy(alpha = 0.1f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "$percent% match",
                                                        color = if (percent == 100) AccentColor else PrimaryColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = dish.description,
                                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            // Count owned ingredients
                                            Text(
                                                text = "You have: $ownedCount of ${dishIngredients.size} ingredients",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (percent == 100) AccentColor else TextColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )

                                            // Highlight missing ingredients
                                            if (missingIngredients.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    missingIngredients.forEach { missing ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color(0xFFFFF0F0))
                                                                .padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "❌ $missing",
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    color = PrimaryColor,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "You have all ingredients! ✨",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = AccentColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Get missing items button (opens bottom sheet)
                                                if (missingIngredients.isNotEmpty()) {
                                                    Button(
                                                        onClick = {
                                                            orderMissingItemsForDish = dish
                                                            orderMissingIngredients = missingIngredients
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = LightGray),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.weight(1f).height(44.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                                    ) {
                                                        Text("Order Missing Items 🛵", color = TextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                                    }
                                                }

                                                // Make this button
                                                Button(
                                                    onClick = {
                                                        navigator.push(PostDecisionScreen(dish, Mood.COMFORT))
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f).height(44.dp)
                                                ) {
                                                    Text("Make this 🧑‍🍳", color = SecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (youMightAlsoLikeDishes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "You might also like:",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = TextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            ),
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }

                                    items(youMightAlsoLikeDishes) { (dish, percent) ->
                                        val dishIngredients = dish.ingredients
                                        val lowercasedOwned = scannedIngredients.map { it.lowercase().trim() }
                                        val missingIngredients = dishIngredients.filter {
                                            !lowercasedOwned.contains(it.lowercase().trim())
                                        }
                                        val ownedCount = dishIngredients.size - missingIngredients.size

                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .shadow(4.dp, RoundedCornerShape(20.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // Category Emoji Placeholder
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(42.dp)
                                                                .background(PrimaryColor.copy(alpha = 0.1f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = when (dish.cuisine) {
                                                                    Cuisine.INDIAN -> "🍛"
                                                                    Cuisine.CHINESE -> "🥢"
                                                                    Cuisine.ITALIAN -> "🍕"
                                                                    Cuisine.MEXICAN -> "🌮"
                                                                    Cuisine.JAPANESE -> "🍣"
                                                                    Cuisine.THAI -> "🍜"
                                                                    else -> "🍱"
                                                                },
                                                                fontSize = 22.sp
                                                            )
                                                        }
                                                        Column {
                                                            Text(
                                                                text = dish.name,
                                                                style = MaterialTheme.typography.titleMedium.copy(
                                                                    color = TextColor,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            )
                                                            Text(
                                                                text = dish.cuisine.name.lowercase().replaceFirstChar { it.uppercase() },
                                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMutedColor)
                                                            )
                                                        }
                                                    }

                                                    // Match Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(30.dp))
                                                            .background(PrimaryColor.copy(alpha = 0.1f))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "$percent% match",
                                                            color = PrimaryColor,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = dish.description,
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )

                                                // Count owned ingredients
                                                Text(
                                                    text = "You have: $ownedCount of ${dishIngredients.size} ingredients",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )

                                                // Highlight missing ingredients
                                                if (missingIngredients.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        missingIngredients.forEach { missing ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color(0xFFFFF0F0))
                                                                    .padding(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "❌ $missing",
                                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                                        color = PrimaryColor,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Get missing items button (opens bottom sheet)
                                                    if (missingIngredients.isNotEmpty()) {
                                                        Button(
                                                            onClick = {
                                                                orderMissingItemsForDish = dish
                                                                orderMissingIngredients = missingIngredients
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = LightGray),
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.weight(1f).height(44.dp),
                                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                                        ) {
                                                            Text("Order Missing Items 🛵", color = TextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                                        }
                                                    }

                                                    // Make this button
                                                    Button(
                                                        onClick = {
                                                            navigator.push(PostDecisionScreen(dish, Mood.COMFORT))
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.weight(1f).height(44.dp)
                                                    ) {
                                                        Text("Make this 🧑‍🍳", color = SecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            }
        }
        if (orderMissingItemsForDish != null) {
            OrderMissingItemsBottomSheet(
                dish = orderMissingItemsForDish!!,
                missingIngredients = orderMissingIngredients,
                onDismiss = { 
                    orderMissingItemsForDish = null
                    orderMissingIngredients = emptyList()
                },
                onViewRecipe = { dish ->
                    orderMissingItemsForDish = null
                    orderMissingIngredients = emptyList()
                    navigator.push(PostDecisionScreen(dish, Mood.COMFORT))
                },
                onAddIngredient = { item ->
                    if (!scannedIngredients.any { it.equals(item, ignoreCase = true) }) {
                        scannedIngredients.add(item)
                        kitchenStorage.saveIngredients(scannedIngredients.toList())
                        orderMissingIngredients = orderMissingIngredients.filter { it != item }
                        if (orderMissingIngredients.isEmpty()) {
                            orderMissingItemsForDish = null
                        }
                        currentStep = KitchenStep.MatchingLoading
                    }
                },
                onAllIngredientsAcquired = {
                    val dish = orderMissingItemsForDish!!
                    orderMissingIngredients.forEach { item ->
                        if (!scannedIngredients.any { it.equals(item, ignoreCase = true) }) {
                            scannedIngredients.add(item)
                        }
                    }
                    kitchenStorage.saveIngredients(scannedIngredients.toList())
                    orderMissingItemsForDish = null
                    orderMissingIngredients = emptyList()
                    navigator.push(PostDecisionScreen(dish, Mood.COMFORT))
                }
            )
        }
    }
}
