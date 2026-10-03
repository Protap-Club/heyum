package com.pgstorm148.heyum.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.api.SuggestionEngineClient
import com.pgstorm148.heyum.engine.WidgetCache
import com.pgstorm148.heyum.firebase.AmbienceRepository
import com.pgstorm148.heyum.firebase.FirebaseAuth
import com.pgstorm148.heyum.firebase.DishRepository
import com.pgstorm148.heyum.firebase.UserRepository
import com.pgstorm148.heyum.firebase.GroupSessionRepository
import com.pgstorm148.heyum.model.*
import com.pgstorm148.heyum.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.pgstorm148.heyum.ui.components.PromoPopup
import org.koin.compose.koinInject
import kotlin.random.Random

// --- Decision Card State ---
sealed class DecisionState {
    object Loading : DecisionState()
    data class Loaded(
        val dishes: List<Dish>,
        val currentIndex: Int,
        val reason: String
    ) : DecisionState()
    object Error : DecisionState()
    object Empty : DecisionState()
}

// --- ScreenModel (ViewModel) ---
class DecisionCardScreenModel(
    private val auth: FirebaseAuth,
    private val suggestionClient: SuggestionEngineClient,
    private val userRepository: UserRepository,
    private val dishRepository: DishRepository,
    private val groupRepository: GroupSessionRepository,
    private val widgetCache: WidgetCache,
    private val preferenceRepository: com.pgstorm148.heyum.engine.PreferenceRepository,
    private val mealLogRepository: com.pgstorm148.heyum.engine.MealLogRepository,
    private val widgetPublisher: com.pgstorm148.heyum.engine.WidgetPublisher
) : ScreenModel {

    private val _state = MutableStateFlow<DecisionState>(DecisionState.Loading)
    val state: StateFlow<DecisionState> = _state.asStateFlow()

    var currentMood by mutableStateOf(Mood.COMFORT)
        private set

    var pendingGroupSessionCode by mutableStateOf<String?>(null)
        private set

    fun clearPendingGroupSessionCode() {
        pendingGroupSessionCode = null
    }

    var mealTypeStr: String = "ALL"
    var consecutiveNahCount by mutableStateOf(0)
    var showVibeReset by mutableStateOf(false)

    fun init(mealType: String) {
        if (this.mealTypeStr != mealType) {
            this.mealTypeStr = mealType
            loadSuggestions()
        }
    }

    init {
        loadSuggestions()
    }

    fun hostGroupSession() {
        val code = (1000..9999).random().toString()
        screenModelScope.launch {
            val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
            val profile = kotlinx.coroutines.withTimeoutOrNull(800) {
                userRepository.getUserProfile(userId).firstOrNull()
            } ?: UserProfile(id = userId, name = "Host")
            // First member of a brand-new session, so any random fun name is unique.
            val hostName = FunNames.pickUnique(emptyList())
            val hostMember = GroupMember(
                userId = userId,
                name = hostName,
                tasteProfile = profile.favoriteCuisines.firstOrNull()?.name ?: "Indian",
                joinedAt = nowMillis(),
                isOrganizer = true,
                dietaryRestrictions = profile.dietaryRestrictions.map { it.name },
                spiceTolerance = profile.spiceTolerance,
                favoriteCuisines = profile.favoriteCuisines.map { it.name }
            )
            val session = GroupSession(
                id = "sess_${userId}_${nowMillis()}",
                organizerId = userId,
                organizerName = hostName,
                sessionName = "Meal Decision",
                createdAt = nowMillis(),
                mealType = mealTypeStr,
                members = listOf(hostMember),
                joinCode = code
            )
            // Firestore applies writes to the local cache immediately and syncs in the
            // background, so navigate right away instead of blocking on a server round-trip;
            // GroupSessionScreen's listener will pick up this session from cache instantly.
            pendingGroupSessionCode = code
            try {
                groupRepository.createSession(session)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun joinGroupSession(code: String, onFail: (String) -> Unit) {
        screenModelScope.launch {
            val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
            val profile = kotlinx.coroutines.withTimeoutOrNull(800) {
                userRepository.getUserProfile(userId).firstOrNull()
            } ?: UserProfile(id = userId, name = "Guest")
            // Look at who's already in the session so we can pick a fun name nobody else has.
            val existingSession = kotlinx.coroutines.withTimeoutOrNull(3000) {
                groupRepository.observeSession(code).firstOrNull()
            }
            val takenNames = existingSession?.members?.map { it.name } ?: emptyList()
            val guestMember = GroupMember(
                userId = userId,
                name = FunNames.pickUnique(takenNames),
                tasteProfile = profile.favoriteCuisines.firstOrNull()?.name ?: "Indian",
                joinedAt = nowMillis(),
                isOrganizer = false,
                dietaryRestrictions = profile.dietaryRestrictions.map { it.name },
                spiceTolerance = profile.spiceTolerance,
                favoriteCuisines = profile.favoriteCuisines.map { it.name }
            )
            // Same reasoning as hostGroupSession: don't block navigation on the server
            // round-trip. GroupSessionScreen shows its own "couldn't find that session"
            // message if the code turns out to be invalid or unreachable.
            pendingGroupSessionCode = code
            try {
                groupRepository.joinSession(code, guestMember)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setMood(mood: Mood) {
        currentMood = mood
        loadSuggestions()
    }

    fun loadSuggestions() {
        screenModelScope.launch {
            _state.value = DecisionState.Loading
            try {
                // Ensure a user profile exists first
                val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
                val profile = kotlinx.coroutines.withTimeoutOrNull(1000) {
                    userRepository.getUserProfile(userId).firstOrNull()
                }
                if (profile == null) {
                    launch {
                        try {
                            userRepository.saveUserProfile(
                                UserProfile(
                                    id = userId,
                                    name = "Gourmet Nom",
                                    dietaryRestrictions = listOf(DietaryRestriction.VEG),
                                    favoriteCuisines = listOf(Cuisine.INDIAN, Cuisine.COMFORT)
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Add some default dishes to Firestore in background if database is empty
                launch {
                    try {
                        prepopulateDishesIfEmpty()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val time = TimeOfDay.EVENING
                val todayLogs = mealLogRepository.getTodayLogs().firstOrNull() ?: emptyList()
                val todayCuisines = todayLogs.map { it.cuisine }
                val todayDishIds = todayLogs.map { it.dishId }
                val hadHeavyLunch = todayLogs.any { 
                    it.mealType == "LUNCH" && it.cuisine in listOf("INDIAN", "COMFORT") 
                }
                val rejectedDishes = preferenceRepository.getRejectedDishes()
                val rejectedIds = rejectedDishes.values
                    .filter { it.mealType == mealTypeStr }
                    .filter { (nowMillis() - it.rejectedAt) / 3600000 < 4 }
                    .map { it.dishId }

                val context = MealContext(
                    timeOfDay = time,
                    weather = "rainy",
                    mood = currentMood,
                    hadHeavyLunch = hadHeavyLunch,
                    todayCuisines = todayCuisines,
                    todayDishIds = todayDishIds,
                    mealTypeStr = mealTypeStr,
                    rejectedDishIds = rejectedIds
                )

                val response = suggestionClient.getSuggestion(userId, context)
                if (response.dishes.isEmpty()) {
                    _state.value = DecisionState.Empty
                } else {
                    _state.value = DecisionState.Loaded(
                        dishes = response.dishes,
                        currentIndex = 0,
                        reason = response.reason
                    )
                }
            } catch (e: Exception) {
                _state.value = DecisionState.Error
            }
        }
    }

    fun onNah() {
        val currentState = _state.value as? DecisionState.Loaded ?: return
        screenModelScope.launch {
            val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
            val currentDish = currentState.dishes[currentState.currentIndex]

            val context = MealContext(TimeOfDay.EVENING, "rainy", currentMood)

            // Run database updates in background so they don't freeze the card swipe animation
            launch {
                try {
                    val mealEntry = MealHistoryEntry(
                        id = currentDish.id,
                        dishName = currentDish.name,
                        cuisine = currentDish.cuisine,
                        accepted = false,
                        timestamp = nowMillis(),
                        context = context
                    )
                    userRepository.updateMealHistory(userId, mealEntry)
                    
                    val currentRejected = preferenceRepository.getRejectedDishes().toMutableMap()
                    currentRejected[currentDish.id] = RejectedDish(
                        dishId = currentDish.id,
                        rejectedAt = nowMillis(),
                        mealType = mealTypeStr
                    )
                    preferenceRepository.saveRejectedDishes(currentRejected)
                    
                    consecutiveNahCount++
                    if (consecutiveNahCount >= 3) {
                        showVibeReset = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Sync Home Screen widget suggestion cache
            try {
                widgetCache.clearCache()
                com.pgstorm148.heyum.engine.triggerWidgetUpdate()
            } catch (e: Exception) {}

            // Revolve through the deck: when we reach the end, wrap back to the start so users
            // never run out of options and can revisit a dish they skipped earlier.
            if (currentState.dishes.isEmpty()) {
                _state.value = DecisionState.Empty
            } else {
                val nextIndex = (currentState.currentIndex + 1) % currentState.dishes.size
                _state.value = currentState.copy(
                    currentIndex = nextIndex,
                    reason = "Chosen dynamically based on your taste settings."
                )
            }
        }
    }

    fun onYum() {
        val currentState = _state.value as? DecisionState.Loaded ?: return
        screenModelScope.launch {
            val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
            val currentDish = currentState.dishes[currentState.currentIndex]

            val context = MealContext(TimeOfDay.EVENING, "rainy", currentMood)
            val userProfile = kotlinx.coroutines.withTimeoutOrNull(500) {
                userRepository.getUserProfile(userId).firstOrNull()
            } ?: UserProfile(id = userId, name = "Gourmet Nom")

            // Run database updates in background
            launch {
                try {
                    consecutiveNahCount = 0
                    val mealEntry = MealHistoryEntry(
                        id = currentDish.id,
                        dishName = currentDish.name,
                        cuisine = currentDish.cuisine,
                        accepted = true,
                        timestamp = nowMillis(),
                        context = context
                    )
                    userRepository.updateMealHistory(userId, mealEntry)
                    
                    val entry = MealLogEntry(
                        id = "log_${nowMillis()}",
                        dishId = currentDish.id,
                        dishName = currentDish.name,
                        cuisine = currentDish.cuisine.name,
                        mealType = mealTypeStr,
                        timestamp = nowMillis(),
                        accepted = true,
                        rating = null
                    )
                    mealLogRepository.insert(entry)
                    // Keep the home-screen widget's "meals logged" pane in step with the log.
                    mealLogRepository.getTodayLogs().firstOrNull()?.let { widgetPublisher.publishMeals(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Sync Home Screen widget suggestion cache
            try {
                widgetCache.clearCache()
                com.pgstorm148.heyum.engine.triggerWidgetUpdate()
            } catch (e: Exception) {}
        }
    }

    private suspend fun prepopulateDishesIfEmpty() {
        // Upsert any pool dish missing from Firestore, not just when the collection is empty.
        // The old empty-only check meant a collection seeded from an older, smaller pool never
        // received dishes added to the pool later — users stayed stuck on the original catalog.
        val currentDishes = dishRepository.getDishesByContext(null, TimeOfDay.EVENING, "rainy").firstOrNull()
        val existingIds = currentDishes.orEmpty().map { it.id }.toSet()
        for (dish in getDefaultDishPool()) {
            if (dish.id !in existingIds) {
                try {
                    dishRepository.saveDish(dish)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

// --- Confetti Animation Particle ---
data class EmojiParticle(
    val id: Int,
    val emoji: String,
    val initialX: Float,
    val initialY: Float,
    val targetX: Float,
    val targetY: Float
)

// --- Screen ---
class DecisionCardScreen(
    val mealType: String = "ALL",
    val showTooltip: Boolean = false,
    val autoJoinGroupCode: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel: DecisionCardScreenModel = koinInject()
        val state by screenModel.state.collectAsState()

        // Server-driven ambience (rain, sunny, etc.). Purely decorative; defaults to off, so this
        // never affects the decision flow. Real-time — Firestore console edits apply live.
        val ambienceRepo: AmbienceRepository = koinInject()
        val ambienceConfig by remember { ambienceRepo.observeAmbience() }
            .collectAsState(initial = AmbienceConfig.Default)

        LaunchedEffect(mealType) {
            screenModel.init(mealType)
        }

        LaunchedEffect(autoJoinGroupCode) {
            if (autoJoinGroupCode != null) {
                screenModel.joinGroupSession(autoJoinGroupCode) { error ->
                    // Log or handle joining error
                }
            }
        }

        val coroutineScope = rememberCoroutineScope()
        var transitionTrigger by remember { mutableStateOf(0) }
        var isSlideLeft by remember { mutableStateOf(true) }
        var isTooltipVisible by remember { mutableStateOf(showTooltip) }
        var showGroupBottomSheet by remember { mutableStateOf(false) }

        LaunchedEffect(screenModel.pendingGroupSessionCode) {
            val code = screenModel.pendingGroupSessionCode
            if (code != null) {
                screenModel.clearPendingGroupSessionCode()
                showGroupBottomSheet = false
                navigator.push(GroupSessionScreen(code))
            }
        }

        // Particles for Confetti
        var particles by remember { mutableStateOf<List<EmojiParticle>>(emptyList()) }
        val particlesAlpha = remember { Animatable(0f) }

        // Yum scale bounce animation
        val yumScale = remember { Animatable(1f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SecondaryColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Section (10%)
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.12f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "yum",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = PrimaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        )
                        Text(
                            text = "your taste, decided",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Header Actions (Settings ⚙️, Kitchen 🥘 & Group Mode 👥)
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings Cog ⚙️
                        IconButton(
                            onClick = { navigator.push(SettingsScreen()) },
                            modifier = Modifier
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Text(
                                text = "⚙️",
                                fontSize = 20.sp,
                                color = TextColor
                            )
                        }

                        // Kitchen Ingredients Icon 🥘
                        IconButton(
                            onClick = { navigator.push(FridgeScannerScreen()) },
                            modifier = Modifier
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Text(
                                text = "🥘",
                                fontSize = 20.sp,
                                color = TextColor
                            )
                        }

                        // Group Mode 👥 Icon Button
                        IconButton(
                            onClick = { showGroupBottomSheet = true },
                            modifier = Modifier
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Text(
                                text = "👥",
                                fontSize = 20.sp,
                                color = TextColor
                            )
                        }
                    }
                }

                // 2. Center Section (72%): Animated Card Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (val currentState = state) {
                        is DecisionState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryColor)
                            }
                        }
                        is DecisionState.Error -> {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Failed to load recommendations.")
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { screenModel.loadSuggestions() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                        is DecisionState.Empty -> {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                var scale by remember { mutableStateOf(1f) }
                                val infiniteTransition = rememberInfiniteTransition()
                                val pulsingScale by infiniteTransition.animateFloat(
                                    initialValue = 0.95f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Text(
                                    text = "Thinking of what you'd love...",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextColor,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.scale(pulsingScale)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Log a craving or change your mood below to get fresh dishes.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { navigator.push(HomeScreen()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                                ) {
                                    Text("Add Craving Log")
                                }
                            }
                        }
                        is DecisionState.Loaded -> {
                            // Voyager custom slide animations for card
                            AnimatedContent(
                                targetState = currentState.currentIndex,
                                transitionSpec = {
                                    if (isSlideLeft) {
                                        // Slide left out, slide in from right
                                        (slideInHorizontally(animationSpec = tween(400, easing = EaseOut)) { width -> width } + fadeIn()) togetherWith
                                                (slideOutHorizontally(animationSpec = tween(400, easing = EaseOut)) { width -> -width } + fadeOut())
                                    } else {
                                        // Fade/Inward transition for mood clicks
                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                    }
                                }
                            ) { index ->
                                val dish = currentState.dishes[index]
                                DecisionCard(
                                    dish = dish,
                                    reason = currentState.reason,
                                    yumScale = yumScale.value,
                                    onNah = {
                                        isSlideLeft = true
                                        screenModel.onNah()
                                    },
                                    onYum = {
                                        // Trigger particle burst
                                        coroutineScope.launch {
                                            // Scale bounce
                                            yumScale.animateTo(0.92f, tween(100))
                                            yumScale.animateTo(1.0f, spring(dampingRatio = 0.5f, stiffness = 300f))

                                            // Fire confetti
                                            val foodEmojis = listOf("🍜", "🥗", "🌮", "🍲", "🍵", "🥪", "🍰", "🍋", "🍊", "🌶️")
                                            particles = (1..25).map { id ->
                                                val angle = Random.nextFloat() * 2 * kotlin.math.PI
                                                val distance = Random.nextFloat() * 300f + 100f
                                                EmojiParticle(
                                                    id = id,
                                                    emoji = foodEmojis.random(),
                                                    initialX = 0f,
                                                    initialY = 0f,
                                                    targetX = (kotlin.math.cos(angle) * distance).toFloat(),
                                                    targetY = (kotlin.math.sin(angle) * distance).toFloat()
                                                )
                                            }
                                            particlesAlpha.snapTo(1f)
                                            // onYum() logs the accepted dish to the daily meal log
                                            // (backend "daily space") before we move on.
                                            screenModel.onYum()
                                            particlesAlpha.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
                                            particles = emptyList()
                                            // Take the user straight into "make it": recipe, ingredient
                                            // check, quick-commerce for missing items, timer, and an
                                            // order-the-dish option at the bottom.
                                            navigator.push(PostDecisionScreen(dish, screenModel.currentMood))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Confetti Particles Overlay
                    if (particles.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            particles.forEach { particle ->
                                val animatedX = remember { Animatable(particle.initialX) }
                                val animatedY = remember { Animatable(particle.initialY) }

                                LaunchedEffect(particle.id) {
                                    animatedX.animateTo(particle.targetX, tween(700, easing = EaseOut))
                                }
                                LaunchedEffect(particle.id) {
                                    animatedY.animateTo(particle.targetY, tween(700, easing = EaseOut))
                                }

                                Text(
                                    text = particle.emoji,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset {
                                            IntOffset(
                                                animatedX.value.dp.roundToPx(),
                                                animatedY.value.dp.roundToPx()
                                            )
                                        }
                                        .scale(particlesAlpha.value)
                                )
                            }
                        }
                    }
                }

                // 3. Bottom Section (18%): Mood Selector
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.22f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "current appetite mood",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMutedColor),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val moods = listOf(
                        MoodItem(Mood.COMFORT, "🍳 Comfort"),
                        MoodItem(Mood.LIGHT, "🥗 Light"),
                        MoodItem(Mood.QUICK, "⚡ Quick"),
                        MoodItem(Mood.SPICY, "🔥 Spicy"),
                        MoodItem(Mood.ADVENTUROUS, "🧪 Adventurous")
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(moods) { item ->
                            val isSelected = item.mood == screenModel.currentMood
                            val chipBg by animateColorAsState(
                                targetValue = if (isSelected) PrimaryColor else Color(0xFFF5F5F5),
                                animationSpec = tween(300)
                            )
                            val chipTextCol by animateColorAsState(
                                targetValue = if (isSelected) SecondaryColor else TextColor,
                                animationSpec = tween(300)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(chipBg)
                                    .clickable {
                                        if (!isSelected) {
                                            isSlideLeft = false // Fade transition on mood click
                                            screenModel.setMood(item.mood)
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = chipTextCol
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Decorative ambience, drawn OVER the content (so rain/snow fall across the whole
            // screen) but UNDER the modals below. Non-interactive: touches pass through to the card.
            AmbienceOverlay(ambienceConfig, Modifier.fillMaxSize())

            // Server-driven promo video (config/promo). Above the content, below the modals.
            PromoPopup()

            if (showGroupBottomSheet) {
                GroupModeBottomSheet(
                    screenModel = screenModel,
                    onDismiss = { showGroupBottomSheet = false }
                )
            }

            if (screenModel.showVibeReset) {
                VibeResetBottomSheet(
                    onDismiss = { 
                        screenModel.showVibeReset = false 
                        screenModel.consecutiveNahCount = 0
                    },
                    onVibeSelected = { mood ->
                        screenModel.setMood(mood)
                        screenModel.showVibeReset = false
                        screenModel.consecutiveNahCount = 0
                    }
                )
            }

            if (isTooltipVisible) {
                TooltipBubble(
                    text = "Tap to change your mood",
                    onDismiss = { isTooltipVisible = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 135.dp)
                )
            }
        }
    }
}

data class MoodItem(val mood: Mood, val label: String)

@Composable
fun DecisionCard(
    dish: Dish,
    reason: String,
    yumScale: Float,
    onNah: () -> Unit,
    onYum: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .fillMaxHeight(0.9f)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFFFF6B35).copy(alpha = 0.15f),
                spotColor = Color(0xFFFF6B35).copy(alpha = 0.25f)
            )
            .border(1.dp, LightGray.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Dish Image & Tag overlay (60% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
            ) {
                // Mock Image box with premium food gradients
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
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    // Simple text representation inside clean gradient since we don't have local drawable resources loaded
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val foodEmoji = when (dish.cuisine) {
                            Cuisine.INDIAN -> "🍛"
                            Cuisine.CHINESE -> "🥢"
                            Cuisine.MEXICAN -> "🌮"
                            Cuisine.HEALTHY -> "🥗"
                            else -> "🍲"
                        }
                        Text(text = foodEmoji, fontSize = 64.sp)
                    }
                }

                // White fade-out at bottom of image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, CardBackgroundColor)
                            )
                        )
                )

                // Cuisine tag pill (Top Right)
                val cuisineIcon = when (dish.cuisine) {
                    Cuisine.INDIAN -> "🍛"
                    Cuisine.COMFORT -> "🍜"
                    Cuisine.CHINESE -> "🥢"
                    Cuisine.MEXICAN -> "🌮"
                    Cuisine.HEALTHY -> "🥗"
                    else -> "🍲"
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${dish.cuisine.name.lowercase().replace("_", " ")} $cuisineIcon",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                    )
                }
            }

            // 2. Info details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Dish Name
                    Text(
                        text = dish.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Calming Reason
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF666666),
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick info row: ⏱️ 25 min | 🌶️🌶️ Medium | 🥬 Vegetarian
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏱️ ${dish.prepTimeMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedColor
                        )
                        Divider(modifier = Modifier.width(1.dp).height(12.dp), color = LightGray)
                        
                        val spicePills = "🌶️".repeat(dish.spiceLevel)
                        val spiceName = when(dish.spiceLevel) {
                            1 -> "Mild"
                            2, 3 -> "Medium"
                            else -> "Spicy"
                        }
                        Text(
                            text = "$spicePills $spiceName",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedColor
                        )
                        Divider(modifier = Modifier.width(1.dp).height(12.dp), color = LightGray)

                        val vegTag = if (dish.dietaryTags.contains(DietaryRestriction.VEG) || dish.dietaryTags.contains(DietaryRestriction.VEGAN)) "🥬 Veg" else "🍖 Non-Veg"
                        Text(
                            text = vegTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedColor
                        )
                    }
                }

                // 3. Choice Buttons side by side (Bottom)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nah Button
                    OutlinedButton(
                        onClick = onNah,
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF777777)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Nah", style = MaterialTheme.typography.labelLarge)
                    }

                    // Yum Button
                    Button(
                        onClick = onYum,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor,
                            contentColor = SecondaryColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .scale(yumScale)
                    ) {
                        Text("Yum! Make it", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun TooltipBubble(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetYFloat by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    val offsetY = offsetYFloat.dp

    Column(
        modifier = modifier
            .offset(y = offsetY)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Box(
            modifier = Modifier
                .size(12.dp, 8.dp)
                .offset(y = (-1).dp)
                .background(PrimaryColor, shape = TriangleShape)
        )
    }
}

val TriangleShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width / 2f, size.height)
    close()
}

@Composable
fun GroupAvatarsRow(members: List<GroupMember>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        members.take(4).forEach { member ->
            val initials = member.name.split(" ").map { it.firstOrNull() }.joinToString("").take(2).uppercase()
            val colors = listOf(
                Color(0xFFCCFF00), Color(0xFF4A90E2), Color(0xFF7B68EE),
                Color(0xFFFF6B6B), Color(0xFF50E3C2), Color(0xFFF5A623)
            )
            val color = colors[member.userId.hashCode().mod(colors.size)]
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                )
            }
        }
        if (members.size > 4) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${members.size - 4}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                )
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val intVal = clean.toLong(16)
    return if (clean.length == 6) {
        Color(intVal or 0xFF000000)
    } else {
        Color(intVal)
    }
}

@Composable
fun GroupModeBottomSheet(
    screenModel: DecisionCardScreenModel,
    onDismiss: () -> Unit
) {
    var joinCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                        .padding(bottom = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Eating together?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = TextMutedColor)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Let's find something everyone loves ❤️",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isCreating = true
                        screenModel.hostGroupSession()
                    },
                    enabled = !isCreating,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(color = SecondaryColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Create Group Session 🚀", color = SecondaryColor, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "— OR —", style = MaterialTheme.typography.labelSmall.copy(color = TextMutedColor))
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Join an existing session:",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextColor),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = {
                            if (it.length <= 4) joinCodeInput = it.filter { char -> char.isDigit() }
                            joinError = null
                        },
                        placeholder = { Text("4-digit code", color = TextMutedColor.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = LightGray,
                            cursorColor = PrimaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (joinCodeInput.length == 4) {
                                isJoining = true
                                joinError = null
                                screenModel.joinGroupSession(joinCodeInput) { err ->
                                    isJoining = false
                                    joinError = err
                                }
                            } else {
                                joinError = "Please enter a 4-digit code"
                            }
                        },
                        enabled = !isJoining,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Join", color = Color.White)
                        }
                    }
                }
                if (joinError != null) {
                    Text(
                        text = joinError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeResetBottomSheet(
    onDismiss: () -> Unit,
    onVibeSelected: (Mood) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Not feeling these? 🤔", color = Color(0xFFF4F4F5), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pick a vibe and we'll refresh your options", color = Color(0xFFA1A1AA), fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val vibes = listOf(
                Pair(Mood.SPICY, "🌶️ Spicy"),
                Pair(Mood.LIGHT, "🥗 Light"),
                Pair(Mood.COMFORT, "🍜 Comfort"),
                Pair(Mood.ADVENTUROUS, "🌍 New Cuisine"),
                Pair(Mood.QUICK, "🎲 Surprise Me")
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // We show them in rows of 2
                for (i in vibes.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VibeButton(
                            modifier = Modifier.weight(1f),
                            text = vibes[i].second,
                            onClick = { onVibeSelected(vibes[i].first) }
                        )
                        if (i + 1 < vibes.size) {
                            VibeButton(
                                modifier = Modifier.weight(1f),
                                text = vibes[i+1].second,
                                onClick = { onVibeSelected(vibes[i+1].first) }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TextButton(onClick = onDismiss) {
                Text("Keep Going", color = Color(0xFFF4F4F5), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VibeButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(60.dp).clickable { onClick() },
        color = Color(0xFF27272A),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color(0xFFF4F4F5), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
