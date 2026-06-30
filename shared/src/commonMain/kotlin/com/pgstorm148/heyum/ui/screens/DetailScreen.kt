package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pgstorm148.heyum.model.FoodSuggestion
import com.pgstorm148.heyum.ui.theme.*

class DetailScreen(private val suggestion: FoodSuggestion) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "recipe",
                            style = MaterialTheme.typography.titleLarge.copy(color = TextColor)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Main Header card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundColor)
                        .padding(20.dp)
                ) {
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleLarge.copy(color = TextColor)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = suggestion.category,
                        style = MaterialTheme.typography.labelSmall.copy(color = AccentColor)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoBadge(label = "Prep", value = suggestion.preparationTime)
                        InfoBadge(label = "Energy", value = suggestion.energy)
                    }
                }

                // Calming note card (Accent deep green highlight)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentColor.copy(alpha = 0.08f))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Why this helps:",
                        style = MaterialTheme.typography.titleMedium.copy(color = AccentColor)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = suggestion.calmingNote,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextColor)
                    )
                }

                // Ingredients card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundColor)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextColor)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    suggestion.ingredients.forEach { ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(PrimaryColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = ingredient,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextColor)
                            )
                        }
                    }
                }

                // Instructions Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundColor)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextColor)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    suggestion.instructions.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.titleMedium.copy(color = PrimaryColor),
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextColor),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
    
    @Composable
    private fun InfoBadge(label: String, value: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(LightGray.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMutedColor)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(color = TextColor)
            )
        }
    }
}
