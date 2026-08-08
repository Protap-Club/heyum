package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.BASE_URL
import com.pgstorm148.heyum.engine.GroupScoreResult
import com.pgstorm148.heyum.engine.TasteProfileEngine
import com.pgstorm148.heyum.firebase.DishRepository
import com.pgstorm148.heyum.firebase.FirebaseAuth
import com.pgstorm148.heyum.firebase.GroupSessionRepository
import com.pgstorm148.heyum.model.*
import com.pgstorm148.heyum.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Owns the whole real-time group flow after a session is created/joined:
 * waiting room -> per-member cuisine+dish selection -> matched result -> cook/order.
 */
class GroupSessionScreen(private val joinCode: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val auth: FirebaseAuth = koinInject()
        val groupRepository: GroupSessionRepository = koinInject()
        val dishRepository: DishRepository = koinInject()
        val widgetPublisher: com.pgstorm148.heyum.engine.WidgetPublisher = koinInject()
        val coroutineScope = rememberCoroutineScope()
        val clipboardManager = LocalClipboardManager.current

        var currentUserId by remember { mutableStateOf("") }
        var session by remember { mutableStateOf<GroupSession?>(null) }
        var candidateDishes by remember { mutableStateOf<List<Dish>>(emptyList()) }
        var matchedDish by remember { mutableStateOf<Dish?>(null) }
        var matchResult by remember { mutableStateOf<GroupScoreResult?>(null) }
        var isLinkCopied by remember { mutableStateOf(false) }
        var draftCuisine by remember { mutableStateOf("") }
        var draftDish by remember { mutableStateOf("") }
        var hasSubmitted by remember { mutableStateOf(false) }
        var loadError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            currentUserId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
            candidateDishes = dishRepository.getDishesByContext(null, TimeOfDay.EVENING, "rainy").firstOrNull() ?: emptyList()
        }

        LaunchedEffect(joinCode) {
            try {
                groupRepository.observeSession(joinCode).collect {
                    session = it
                    // Surface a live session on the home-screen widget so members can hop back in;
                    // drop it again once the session is over.
                    if (it != null && it.status != "ENDED" && it.status != "COMPLETED") {
                        widgetPublisher.publishSession(it.joinCode, it.sessionName)
                    } else {
                        widgetPublisher.clearSession()
                    }
                }
            } catch (e: Exception) {
                loadError = e.message ?: "Lost connection while loading this session."
            }
        }

        // If nothing shows up after a while, the code is probably wrong or we're offline —
        // don't leave the user staring at a spinner forever.
        LaunchedEffect(joinCode) {
            delay(12000)
            if (session == null) {
                loadError = "Couldn't find a session with this code. Check the code and your connection, then try again."
            }
        }

        // Keep this user's draft in sync with whatever's already stored for them (e.g. re-entering the screen)
        LaunchedEffect(session?.selectedCuisines?.get(currentUserId), session?.selectedDishes?.get(currentUserId)) {
            val storedCuisine = session?.selectedCuisines?.get(currentUserId)
            val storedDish = session?.selectedDishes?.get(currentUserId)
            if (!storedCuisine.isNullOrBlank()) draftCuisine = storedCuisine
            if (!storedDish.isNullOrBlank()) draftDish = storedDish
            hasSubmitted = !storedDish.isNullOrBlank()
        }

        // Once every member has picked, compute (or read) the group match
        LaunchedEffect(session?.selectedCuisines, session?.selectedDishes, candidateDishes) {
            val currentSession = session ?: return@LaunchedEffect
            if (currentSession.finalDish.isNotEmpty() || candidateDishes.isEmpty()) return@LaunchedEffect
            val allReady = currentSession.members.isNotEmpty() && currentSession.members.all { member ->
                !currentSession.selectedCuisines[member.userId].isNullOrBlank() &&
                    !currentSession.selectedDishes[member.userId].isNullOrBlank()
            }
            if (allReady) {
                val match = TasteProfileEngine.findGroupMatch(currentSession, candidateDishes)
                if (match != null) {
                    groupRepository.makeDecision(joinCode, match.first.id)
                }
            }
        }

        // Resolve the final dish once it's set, for everyone observing
        LaunchedEffect(session?.finalDish, candidateDishes) {
            val currentSession = session ?: return@LaunchedEffect
            if (currentSession.finalDish.isNotEmpty() && candidateDishes.isNotEmpty()) {
                val dish = candidateDishes.firstOrNull { it.id == currentSession.finalDish }
                if (dish != null) {
                    matchedDish = dish
                    matchResult = TasteProfileEngine.scoreDishForGroup(currentSession, dish)
                }
            }
        }

        val currentSession = session
        val isOrganizer = currentSession != null && currentSession.organizerId == currentUserId

        Scaffold(
            containerColor = SecondaryColor,
            topBar = {
                TopAppBar(
                    title = { Text("Group Session", color = TextColor, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Text("←", color = TextColor, fontSize = 20.sp)
                        }
                    },
                    actions = {
                        if (isOrganizer && currentSession?.status != "ENDED") {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    groupRepository.endSession(joinCode)
                                    navigator.pop()
                                }
                            }) {
                                Text("End Session", color = Color.Red)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryColor)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (currentSession == null) {
                    if (loadError != null) {
                        ErrorContent(message = loadError!!, onGoBack = { navigator.pop() })
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryColor)
                        }
                    }
                } else when {
                    currentSession.status == "ENDED" -> EndedContent(onDone = { navigator.pop() })
                    matchedDish != null -> MatchedContent(
                        dish = matchedDish!!,
                        result = matchResult,
                        onCook = { navigator.push(PostDecisionScreen(matchedDish!!, Mood.COMFORT)) },
                        onOrder = { navigator.push(PostDecisionScreen(matchedDish!!, Mood.COMFORT, initialShowOrderTab = true)) }
                    )
                    currentSession.status == "ACTIVE" -> SelectionContent(
                        session = currentSession,
                        currentUserId = currentUserId,
                        draftCuisine = draftCuisine,
                        onCuisineChange = { draftCuisine = it },
                        draftDish = draftDish,
                        onDishChange = { draftDish = it },
                        hasSubmitted = hasSubmitted,
                        onHeyYum = {
                            coroutineScope.launch {
                                groupRepository.updateSelection(joinCode, currentUserId, draftCuisine, draftDish)
                                hasSubmitted = true
                            }
                        }
                    )
                    else -> WaitingRoomContent(
                        session = currentSession,
                        isOrganizer = isOrganizer,
                        isLinkCopied = isLinkCopied,
                        onCopyLink = {
                            val link = "https://$BASE_URL/join/${currentSession.joinCode}"
                            clipboardManager.setText(AnnotatedString("Join my Heyum group and let's decide what to eat! $link"))
                            isLinkCopied = true
                            coroutineScope.launch {
                                delay(2000)
                                isLinkCopied = false
                            }
                        },
                        onShare = {
                            val link = "https://$BASE_URL/join/${currentSession.joinCode}"
                            shareText("Join my Heyum group and let's decide what to eat! $link")
                        },
                        onStart = { coroutineScope.launch { groupRepository.startCollaboration(joinCode) } },
                        onLeave = {
                            coroutineScope.launch {
                                groupRepository.leaveSession(joinCode, currentUserId)
                                navigator.pop()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingRoomContent(
    session: GroupSession,
    isOrganizer: Boolean,
    isLinkCopied: Boolean,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onStart: () -> Unit,
    onLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Share this code to invite people",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = session.joinCode,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = PrimaryColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                letterSpacing = 4.sp
            )
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCopyLink,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isLinkCopied) "Copied! ✓" else "Copy Link")
            }
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Share 🔗", color = SecondaryColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Group Members (${session.members.size}):",
            style = MaterialTheme.typography.labelLarge.copy(color = TextColor),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        GroupAvatarsRow(session.members)

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(session.members) { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(member.name, color = TextColor)
                    if (member.isOrganizer) {
                        Text("Organizer", color = AccentColor, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (isOrganizer) {
            Button(
                onClick = onStart,
                enabled = session.members.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (session.members.size >= 2) "Let's Start 🚀" else "Waiting for more people…",
                    color = SecondaryColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            OutlinedButton(
                onClick = onLeave,
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Leave Group Session 🚪")
            }
        }
    }
}

@Composable
private fun SelectionContent(
    session: GroupSession,
    currentUserId: String,
    draftCuisine: String,
    onCuisineChange: (String) -> Unit,
    draftDish: String,
    onDishChange: (String) -> Unit,
    hasSubmitted: Boolean,
    onHeyYum: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "What are you craving?",
            style = MaterialTheme.typography.titleLarge.copy(color = TextColor, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pick a cuisine, then tell us the dish. We'll find something everyone loves.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Cuisine", style = MaterialTheme.typography.labelLarge.copy(color = TextColor))
        Spacer(modifier = Modifier.height(8.dp))
        CuisineDropdownField(selected = draftCuisine, enabled = !hasSubmitted, onSelected = onCuisineChange)

        Spacer(modifier = Modifier.height(20.dp))

        Text("Dish", style = MaterialTheme.typography.labelLarge.copy(color = TextColor))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draftDish,
            onValueChange = onDishChange,
            enabled = !hasSubmitted,
            placeholder = { Text("e.g. Butter Chicken", color = TextMutedColor.copy(alpha = 0.6f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = LightGray,
                cursorColor = PrimaryColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasSubmitted) {
            Button(
                onClick = onHeyYum,
                enabled = draftCuisine.isNotBlank() && draftDish.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Hey Yum! 🙌", color = SecondaryColor, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(
                text = "You're in! Waiting for everyone else…",
                style = MaterialTheme.typography.bodyMedium.copy(color = AccentColor, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Who's ready:",
            style = MaterialTheme.typography.labelLarge.copy(color = TextColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(session.members) { member ->
                val ready = !session.selectedCuisines[member.userId].isNullOrBlank() &&
                    !session.selectedDishes[member.userId].isNullOrBlank()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(member.name, color = TextColor)
                    Text(if (ready) "Yum! ✓" else "Choosing…", color = if (ready) PrimaryColor else TextMutedColor)
                }
            }
        }
    }
}

@Composable
private fun MatchedContent(
    dish: Dish,
    result: GroupScoreResult?,
    onCook: () -> Unit,
    onOrder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("This is what will make you say Yum!", style = MaterialTheme.typography.titleMedium.copy(color = TextMutedColor))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = dish.name,
            style = MaterialTheme.typography.headlineMedium.copy(color = PrimaryColor, fontWeight = FontWeight.ExtraBold)
        )
        if (result != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(result.score * 100).toInt()}% match for everyone",
                style = MaterialTheme.typography.bodyMedium.copy(color = AccentColor)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCook,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("🧑‍🍳 Let's Cook")
            }
            Button(
                onClick = onOrder,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("🛵 Let's Order", color = SecondaryColor)
            }
        }
    }
}

@Composable
private fun EndedContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("This session has ended", style = MaterialTheme.typography.titleLarge.copy(color = TextColor))
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
            Text("Done", color = SecondaryColor)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onGoBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onGoBack, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
            Text("Go Back", color = SecondaryColor)
        }
    }
}

@Composable
private fun CuisineDropdownField(selected: String, enabled: Boolean, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LightGray),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = selected.takeIf { it.isNotBlank() }
                    ?.let { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    ?: "Choose a cuisine"
                Text(text = label, color = if (selected.isBlank()) TextMutedColor else TextColor)
                Text(if (expanded) "▲" else "▼", color = TextMutedColor, fontSize = 12.sp)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Cuisine.values().forEach { cuisine ->
                val label = cuisine.name.lowercase().replaceFirstChar { it.uppercase() }
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(cuisine.name)
                        expanded = false
                    }
                )
            }
        }
    }
}
