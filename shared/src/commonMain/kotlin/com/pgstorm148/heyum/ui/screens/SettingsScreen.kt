package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.pgstorm148.heyum.model.NotificationFrequency
import com.pgstorm148.heyum.model.UserProfile
import com.pgstorm148.heyum.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val auth: FirebaseAuth = koinInject()
        val userRepository: UserRepository = koinInject()
        val preferenceRepository: com.pgstorm148.heyum.engine.PreferenceRepository = koinInject()
        val coroutineScope = rememberCoroutineScope()

        var profile by remember { mutableStateOf<UserProfile?>(null) }
        var isSaving by remember { mutableStateOf(false) }
        var showSaveFeedback by remember { mutableStateOf(false) }

        // Local UI states mirroring profile settings
        var frequency by remember { mutableStateOf(NotificationFrequency.STANDARD) }
        var quietHoursEnabled by remember { mutableStateOf(false) }
        var quietHoursStart by remember { mutableStateOf("23:00") }
        var quietHoursEnd by remember { mutableStateOf("08:00") }
        
        var mealRemindersEnabled by remember { mutableStateOf(true) }
        var reorderSuggestionsEnabled by remember { mutableStateOf(true) }
        var wasteAlertsEnabled by remember { mutableStateOf(true) }
        var groupInvitesEnabled by remember { mutableStateOf(true) }

        // Fetch user profile on enter
        LaunchedEffect(Unit) {
            val userId = auth.currentUser.firstOrNull()?.uid ?: "default_user_123"
            userRepository.getUserProfile(userId).collect { fetchedProfile ->
                if (fetchedProfile != null && profile == null) {
                    profile = fetchedProfile
                    frequency = fetchedProfile.notificationFrequency
                    quietHoursEnabled = fetchedProfile.quietHoursEnabled
                    quietHoursStart = fetchedProfile.quietHoursStart
                    quietHoursEnd = fetchedProfile.quietHoursEnd
                    mealRemindersEnabled = fetchedProfile.mealRemindersEnabled
                    reorderSuggestionsEnabled = fetchedProfile.reorderSuggestionsEnabled
                    wasteAlertsEnabled = fetchedProfile.wasteAlertsEnabled
                    groupInvitesEnabled = fetchedProfile.groupInvitesEnabled
                }
            }
        }

        // Save Function
        val saveSettings = {
            val currentProfile = profile
            if (currentProfile != null) {
                isSaving = true
                coroutineScope.launch {
                    val updatedProfile = currentProfile.copy(
                        notificationFrequency = frequency,
                        quietHoursEnabled = quietHoursEnabled,
                        quietHoursStart = quietHoursStart,
                        quietHoursEnd = quietHoursEnd,
                        mealRemindersEnabled = mealRemindersEnabled,
                        reorderSuggestionsEnabled = reorderSuggestionsEnabled,
                        wasteAlertsEnabled = wasteAlertsEnabled,
                        groupInvitesEnabled = groupInvitesEnabled
                    )
                    userRepository.saveUserProfile(updatedProfile)
                    isSaving = false
                    showSaveFeedback = true
                    delay(1500)
                    showSaveFeedback = false
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.background(Color.Transparent, CircleShape)
                        ) {
                            Text("←", color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryColor)
                )
            },
            containerColor = SecondaryColor
        ) { paddingValues ->
            if (profile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Intro Tip Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔔", fontSize = 28.sp)
                            Column {
                                Text(
                                    text = "Quiet & Helpful",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Set reminders that match your rhythm. We hate spam just as much as you do.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
                                )
                            }
                        }
                    }

                    // 1. Notification Frequency Section
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Notification Frequency",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NotificationFrequency.values().forEach { freq ->
                                    val isSelected = freq == frequency
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryColor else LightGray.copy(alpha = 0.5f))
                                            .clickable {
                                                frequency = freq
                                                saveSettings()
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when(freq) {
                                                NotificationFrequency.OFTEN -> "Often"
                                                NotificationFrequency.STANDARD -> "Standard"
                                                NotificationFrequency.MINIMAL -> "Minimal"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSelected) SecondaryColor else TextColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Quiet Hours Section
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Quiet Hours",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Block notifications during quiet times",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
                                    )
                                }
                                Switch(
                                    checked = quietHoursEnabled,
                                    onCheckedChange = {
                                        quietHoursEnabled = it
                                        saveSettings()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SecondaryColor,
                                        checkedTrackColor = PrimaryColor,
                                        uncheckedThumbColor = LightGray,
                                        uncheckedTrackColor = Color.Transparent
                                    )
                                )
                            }

                            AnimatedVisibility(visible = quietHoursEnabled) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Start time
                                        OutlinedTextField(
                                            value = quietHoursStart,
                                            onValueChange = {
                                                quietHoursStart = it
                                                saveSettings()
                                            },
                                            label = { Text("From (e.g. 23:00)") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryColor,
                                                unfocusedBorderColor = LightGray,
                                                cursorColor = PrimaryColor
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // End time
                                        OutlinedTextField(
                                            value = quietHoursEnd,
                                            onValueChange = {
                                                quietHoursEnd = it
                                                saveSettings()
                                            },
                                            label = { Text("To (e.g. 08:00)") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryColor,
                                                unfocusedBorderColor = LightGray,
                                                cursorColor = PrimaryColor
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Notification Types Toggle Section
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Notification Categories",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Meal Reminders
                            ToggleRow(
                                title = "Meal Reminders",
                                subtitle = "Lunch nudges and rain context ideas",
                                checked = mealRemindersEnabled,
                                onCheckedChange = {
                                    mealRemindersEnabled = it
                                    saveSettings()
                                }
                            )

                            // Re-order Suggestions
                            ToggleRow(
                                title = "Re-order Suggestions",
                                subtitle = "Quick shortcuts for dishes you loved last week",
                                checked = reorderSuggestionsEnabled,
                                onCheckedChange = {
                                    reorderSuggestionsEnabled = it
                                    saveSettings()
                                }
                            )

                            // Waste Alerts
                            ToggleRow(
                                title = "Fridge Waste Alerts",
                                subtitle = "Alerts for scanned ingredients about to expire",
                                checked = wasteAlertsEnabled,
                                onCheckedChange = {
                                    wasteAlertsEnabled = it
                                    saveSettings()
                                }
                            )

                            // Group Invites
                            ToggleRow(
                                title = "Group Invites",
                                subtitle = "When friends invite you to decide meals together",
                                checked = groupInvitesEnabled,
                                onCheckedChange = {
                                    groupInvitesEnabled = it
                                    saveSettings()
                                }
                            )
                        }
                    }

                    // Saved Feedback indicator
                    AnimatedVisibility(
                        visible = showSaveFeedback || isSaving,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentColor)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSaving) "Saving..." else "Settings saved successfully! ✨",
                                color = SecondaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 4. Data Management
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Data Management",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            preferenceRepository.clearPreferences()
                                            showSaveFeedback = true
                                            delay(1500)
                                            showSaveFeedback = false
                                        } catch(e: Exception) {}
                                    }
                                },
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Reset All Preferences")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextColor
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SecondaryColor,
                checkedTrackColor = PrimaryColor,
                uncheckedThumbColor = LightGray,
                uncheckedTrackColor = Color.Transparent
            )
        )
    }
}
