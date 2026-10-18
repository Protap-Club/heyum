package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.firebase.FirebaseAuth
import com.pgstorm148.heyum.firebase.FirebaseFirestore
import com.pgstorm148.heyum.firebase.AuthRepository
import com.pgstorm148.heyum.model.Craving
import com.pgstorm148.heyum.model.FirebaseUser
import com.pgstorm148.heyum.model.FoodSuggestion
import com.pgstorm148.heyum.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val auth: FirebaseAuth = koinInject()
        val authRepository: AuthRepository = koinInject()
        val db: FirebaseFirestore = koinInject()
        val widgetPublisher: com.pgstorm148.heyum.engine.WidgetPublisher = koinInject()
        val coroutineScope = rememberCoroutineScope()

        val currentUserState by authRepository.currentUser.collectAsState()
        val activeUser = currentUserState ?: auth.currentCachedUser

        var cravings by remember { mutableStateOf<List<Craving>>(emptyList()) }
        var cravingText by remember { mutableStateOf("") }
        var selectedMood by remember { mutableStateOf("Warm & Cozy") }
        var notesText by remember { mutableStateOf("") }

        val moods = listOf("Warm & Cozy", "Fresh & Light", "Sweet Treat", "Calming Tea")

        // Load suggestions static pool
        val suggestions = remember { getMockSuggestions() }

        LaunchedEffect(activeUser) {
            val uid = activeUser?.uid
            if (uid != null) {
                db.observeCravings(uid).collect { list ->
                    cravings = list
                    widgetPublisher.publishCravings(list)
                }
            }
        }

        if (activeUser == null) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "nom journal",
                                style = MaterialTheme.typography.titleLarge.copy(color = TextColor)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryColor)
                    )
                },
                containerColor = SecondaryColor
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(CardBackgroundColor)
                            .padding(32.dp)
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Please log in to view your cravings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextColor,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To keep your nom journal secure and synced, please authenticate with your account.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                navigator.replace(WelcomeScreen())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                text = "Go to Login",
                                color = SecondaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            return
        }

        // Calming notes matching logic: match based on mood or search keywords
        val matchedSuggestions = remember(selectedMood, cravingText, cravings) {
            val query = cravingText.lowercase()
            val filtered = suggestions.filter {
                it.category.lowercase() == selectedMood.lowercase() || 
                (query.isNotEmpty() && (it.title.lowercase().contains(query) || it.description.lowercase().contains(query)))
            }
            if (filtered.isEmpty()) suggestions.take(2) else filtered
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "nom journal",
                            style = MaterialTheme.typography.titleLarge.copy(color = TextColor)
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                auth.signOut()
                                navigator.replace(WelcomeScreen())
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = PrimaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SecondaryColor
                    )
                )
            },
            containerColor = SecondaryColor
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Intro Calming Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackgroundColor)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "What is your body asking for today?",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Write a quick note of what you are craving, select a mood, and let us recommend something fresh.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
                        )
                    }
                }

                // New Craving Input Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackgroundColor)
                            .padding(20.dp)
                    ) {
                        OutlinedTextField(
                            value = cravingText,
                            onValueChange = { cravingText = it },
                            placeholder = { Text("Craving something... (e.g. ramen, crunchy salad)", color = TextMutedColor.copy(alpha = 0.7f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = LightGray,
                                cursorColor = PrimaryColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mood selection chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            moods.forEach { mood ->
                                val isSelected = mood == selectedMood
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) PrimaryColor else LightGray.copy(alpha = 0.5f))
                                        .clickable { selectedMood = mood }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = mood,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) SecondaryColor else TextColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("Any specific notes? (Optional context)", color = TextMutedColor.copy(alpha = 0.7f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = LightGray,
                                cursorColor = PrimaryColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (cravingText.isBlank()) return@Button
                                val craving = Craving(
                                    userId = activeUser.uid,
                                    foodName = cravingText,
                                    mood = selectedMood,
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    notes = notesText
                                )
                                cravingText = ""
                                notesText = ""
                                coroutineScope.launch {
                                    try {
                                        db.saveCraving(craving)
                                    } catch (e: Exception) {
                                        // Firestore queues writes offline and syncs later, so a
                                        // failure here is genuinely unexpected. Log it — silently
                                        // swallowing made a broken *read* (missing Firestore index)
                                        // look like a broken write for a long time.
                                        e.printStackTrace()
                                    }
                                }
                            },
                            enabled = cravingText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryColor,
                                contentColor = SecondaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Log Craving")
                        }
                    }
                }

                // Recommendations Panel (Accent Green theme for freshness)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AccentColor.copy(alpha = 0.08f))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Calming Nom Recommendations",
                            style = MaterialTheme.typography.titleMedium.copy(color = AccentColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clean, nourishing ideas chosen for you:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMutedColor)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        matchedSuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackgroundColor)
                                    .clickable { navigator.push(DetailScreen(suggestion)) }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.title,
                                        style = MaterialTheme.typography.titleMedium.copy(color = TextColor)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = suggestion.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = suggestion.energy,
                                        style = MaterialTheme.typography.labelSmall.copy(color = AccentColor)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // Cravings list title
                if (cravings.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cravings History",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextColor)
                        )
                    }
                }

                // Cravings journal entries list
                items(cravings) { craving ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackgroundColor)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = craving.foodName,
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(LightGray)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = craving.mood,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedColor
                                    )
                                }
                            }
                            if (craving.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = craving.notes,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
                                )
                            }
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                db.deleteCraving(craving.id)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                tint = TextMutedColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// Simple clock model implementation to avoid adding datetime libraries dependency
object Clock {
    object System {
        fun now(): Instant = Instant(nowMillis())
    }
}

data class Instant(val epochMilliseconds: Long) {
    fun toEpochMilliseconds() = epochMilliseconds
}

// Expect function to get current time millis in platform specific way
expect fun nowMillis(): Long
expect fun getCurrentHour(): Int

fun getMockSuggestions(): List<FoodSuggestion> {
    return listOf(
        FoodSuggestion(
            id = "1",
            title = "Calming Ginger Ramen",
            category = "Warm & Cozy",
            description = "A warm, soothing ginger broth with fresh buckwheat noodles and soft steamed tofu.",
            ingredients = listOf("Buckwheat noodles (soba)", "Fresh ginger", "Silken tofu", "Shiitake mushrooms", "Bok choy", "Miso paste"),
            instructions = listOf(
                "Simmer sliced ginger and shiitake mushrooms in vegetable broth for 15 minutes.",
                "Stir in miso paste and cook buckwheat noodles until soft.",
                "Serve hot, topped with steamed tofu and baby bok choy."
            ),
            preparationTime = "20 mins",
            energy = "380 kcal",
            calmingNote = "Ginger naturally soothes the stomach and warm broth provides immediate comforting sensations."
        ),
        FoodSuggestion(
            id = "2",
            title = "Garden Harvest Buddha Bowl",
            category = "Fresh & Light",
            description = "Crisp massaged kale, roasted sweet potato, creamy avocado, and refreshing tahini sauce.",
            ingredients = listOf("Fresh kale", "Roasted sweet potato cubes", "Ripe avocado", "Cooked quinoa", "Tahini", "Lemon juice"),
            instructions = listOf(
                "Massage kale with lemon juice and a pinch of salt to soften.",
                "Layer cooked quinoa, roasted sweet potatoes, and avocado slices in a bowl.",
                "Drizzle with clean lemon-tahini dressing."
            ),
            preparationTime = "15 mins",
            energy = "420 kcal",
            calmingNote = "Dark leafy greens contain magnesium which helps regulate stress and supports cognitive function."
        ),
        FoodSuggestion(
            id = "3",
            title = "Warm Honey & Berry Oat Crumble",
            category = "Sweet Treat",
            description = "Warm toasted oats baked with wild blueberries, raw honey, and aromatic cinnamon.",
            ingredients = listOf("Rolled oats", "Wild blueberries", "Raw honey", "Coconut oil", "Cinnamon", "Almond slivers"),
            instructions = listOf(
                "Mix oats, cinnamon, melted coconut oil, and honey in a bowl.",
                "Place berries in a baking dish, cover with the oat mixture.",
                "Bake at 180°C for 20 minutes until berries bubble and oats are golden."
            ),
            preparationTime = "25 mins",
            energy = "310 kcal",
            calmingNote = "Blueberries are rich in antioxidants, and honey combined with warm oats releases steady serotonin."
        ),
        FoodSuggestion(
            id = "4",
            title = "Chamomile Citrus Mint Infusion",
            category = "Calming Tea",
            description = "A delicate blend of soothing chamomile blossoms, fresh garden mint, and orange peel.",
            ingredients = listOf("Chamomile blossoms", "Fresh mint leaves", "Orange zest", "Filtered hot water"),
            instructions = listOf(
                "Place chamomile and mint in a warm teapot.",
                "Pour hot water (approx 90°C) and let steep for 7-10 minutes.",
                "Strain into a mug and add orange peel for a warm citrus aroma."
            ),
            preparationTime = "8 mins",
            energy = "5 kcal",
            calmingNote = "Chamomile contains apigenin, an antioxidant that binds to receptors in your brain and promotes sleepiness."
        )
    )
}
