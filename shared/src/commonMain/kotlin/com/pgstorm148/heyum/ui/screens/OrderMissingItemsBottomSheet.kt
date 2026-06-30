package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgstorm148.heyum.model.Dish
import com.pgstorm148.heyum.ui.theme.AccentColor
import com.pgstorm148.heyum.ui.theme.LightGray
import com.pgstorm148.heyum.ui.theme.PrimaryColor
import com.pgstorm148.heyum.ui.theme.TextColor
import com.pgstorm148.heyum.ui.theme.TextMutedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderMissingItemsBottomSheet(
    dish: Dish,
    missingIngredients: List<String>,
    onDismiss: () -> Unit,
    onViewRecipe: (Dish) -> Unit,
    onAddIngredient: (String) -> Unit,
    onAllIngredientsAcquired: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var selectedApp by remember { mutableStateOf("Blinkit") }
    val orderedItems = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Order Missing Items",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Where do you want to order from?",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedColor)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Blinkit", "Zepto", "Instamart", "BigBasket").forEach { app ->
                    val isSelected = selectedApp == app
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryColor else LightGray.copy(alpha = 0.5f))
                            .clickable { selectedApp = app }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = app,
                            color = if (isSelected) Color.White else TextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "${orderedItems.size} of ${missingIngredients.size} items ordered",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(missingIngredients) { item ->
                    val isOrdered = orderedItems.contains(item)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOrdered) LightGray.copy(alpha = 0.5f) else Color(0xFFFFF0F0))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isOrdered,
                                    onCheckedChange = {
                                        if (it) orderedItems.add(item) else orderedItems.remove(item)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryColor)
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (isOrdered) TextDecoration.LineThrough else null,
                                        color = TextColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    val url = when (selectedApp) {
                                        "Blinkit" -> "https://blinkit.com/search?q=${item.replace(" ", "%20")}"
                                        "Zepto" -> "https://www.zeptonow.com/search?query=${item.replace(" ", "%20")}"
                                        "Instamart" -> "https://www.swiggy.com/instamart/search?q=${item.replace(" ", "%20")}"
                                        else -> "https://www.bigbasket.com/ps/?q=${item.replace(" ", "%20")}"
                                    }
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                enabled = !isOrdered,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Order", fontSize = 12.sp)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 48.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = { onAddIngredient(item) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("+ Found it in fridge", color = AccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onViewRecipe(dish) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Skip & View Recipe", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = onAllIngredientsAcquired,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I have all items ✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
