package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.firebase.FirebaseAuth
import com.pgstorm148.heyum.firebase.UserRepository
import com.pgstorm148.heyum.firebase.DishRepository
import com.pgstorm148.heyum.model.*
import com.pgstorm148.heyum.ui.theme.PrimaryColor
import com.pgstorm148.heyum.ui.theme.SecondaryColor
import com.pgstorm148.heyum.ui.theme.TextColor
import com.pgstorm148.heyum.ui.theme.TextMutedColor
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class WelcomeScreen(val initialNotificationPayload: NotificationPayload? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val auth: FirebaseAuth = koinInject()
        val userRepository: UserRepository = koinInject()
        val dishRepository: DishRepository = koinInject()
        val coroutineScope = rememberCoroutineScope()
        
        var phoneNumber by remember { mutableStateOf("") }
        var verificationCode by remember { mutableStateOf("") }
        var verificationId by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Helper lambda to route deep link or standard screen
        val handlePayloadNavigation = remember {
            { profile: UserProfile ->
                val payload = initialNotificationPayload
                if (payload != null && profile.onboardingCompleted) {
                    coroutineScope.launch {
                        when (payload.type) {
                            "nudge", "reminder", "reorder", "reorder_direct" -> {
                                if (payload.dishId != null) {
                                    val dishes = dishRepository.getDishesByContext(null, TimeOfDay.EVENING, "rainy").firstOrNull() ?: emptyList()
                                    val targetDish = dishes.firstOrNull { it.id == payload.dishId } ?: dishes.firstOrNull()
                                    if (targetDish != null) {
                                        navigator.replace(MealSelectorScreen())
                                        navigator.push(ConfirmationScreen(
                                            mealLogId = "",
                                            dishName = targetDish.name,
                                            mealTypeStr = "Dinner",
                                            mealEmoji = "🍽️"
                                        ))
                                    } else {
                                        navigator.replace(MealSelectorScreen())
                                    }
                                } else {
                                    navigator.replace(MealSelectorScreen())
                                }
                            }
                            "group" -> {
                                // DecisionCardScreen auto-joins via payload.groupId, then hands off
                                // to GroupSessionScreen once the join succeeds (see pendingGroupSessionCode).
                                navigator.replace(MealSelectorScreen())
                                navigator.push(DecisionCardScreen(mealType = "ALL", autoJoinGroupCode = payload.groupId))
                            }
                            "waste" -> {
                                navigator.replace(MealSelectorScreen())
                                navigator.push(FridgeScannerScreen(prepopulatedIngredient = payload.ingredient))
                            }
                            else -> {
                                navigator.replace(MealSelectorScreen())
                            }
                        }
                    }
                } else if (profile.onboardingCompleted) {
                    navigator.replace(MealSelectorScreen())
                } else {
                    navigator.replace(OnboardingScreen())
                }
            }
        }

        // Automatically navigate if user is already logged in
        LaunchedEffect(Unit) {
            val user = auth.currentUser.firstOrNull()
            if (user != null) {
                val profile = try {
                    kotlinx.coroutines.withTimeoutOrNull(1500) {
                        userRepository.getUserProfile(user.uid).firstOrNull()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                if (profile != null) {
                    handlePayloadNavigation(profile)
                } else {
                    navigator.replace(OnboardingScreen())
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SecondaryColor)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Calming App Branding
                Text(
                    text = "yum.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = PrimaryColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "your taste, decided",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                if (verificationId == null) {
                    // Step 1: Input Phone Number
                    Text(
                        text = "Tired of 'What do you want to eat?'",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextColor, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "We decide. You eat. Simple.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it; errorMessage = null },
                        label = { Text("Phone Number", color = TextMutedColor) },
                        placeholder = { Text("+91 98765 43210", color = TextMutedColor.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = TextMutedColor.copy(alpha = 0.5f),
                            focusedLabelColor = PrimaryColor,
                            cursorColor = PrimaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(color = PrimaryColor)
                    } else {
                        Button(
                            onClick = {
                                if (phoneNumber.isBlank()) {
                                    errorMessage = "Please enter your phone number"
                                    return@Button
                                }
                                isLoading = true
                                try {
                                    auth.verifyPhoneNumber(
                                        phoneNumber = phoneNumber,
                                        onCodeSent = { id ->
                                            verificationId = id
                                            isLoading = false
                                        },
                                        onFailed = { err ->
                                            errorMessage = err
                                            isLoading = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Phone verification failed"
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryColor,
                                contentColor = SecondaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Send OTP",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else {
                    // Step 2: Enter OTP Code
                    Text(
                        text = "Enter the 6-digit OTP sent to",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextColor, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it; errorMessage = null },
                        label = { Text("6-Digit OTP Code", color = TextMutedColor) },
                        placeholder = { Text("123456", color = TextMutedColor.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = TextMutedColor.copy(alpha = 0.5f),
                            focusedLabelColor = PrimaryColor,
                            cursorColor = PrimaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(color = PrimaryColor)
                    } else {
                        Button(
                            onClick = {
                                if (verificationCode.isBlank()) {
                                    errorMessage = "Please enter the OTP code"
                                    return@Button
                                }
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val loggedInUser = auth.signInWithCode(verificationId!!, verificationCode)
                                        val profile = userRepository.getUserProfile(loggedInUser.uid).firstOrNull()
                                        if (profile != null) {
                                            handlePayloadNavigation(profile)
                                        } else {
                                            navigator.replace(OnboardingScreen())
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Authentication failed"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryColor,
                                contentColor = SecondaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Verify & Start Deciding",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                verificationId = null
                                verificationCode = ""
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = "Change Phone Number",
                                color = TextColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
