package com.pgstorm148.heyum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgstorm148.heyum.delivery.DeliveryApp
import com.pgstorm148.heyum.delivery.DeliveryCategory
import com.pgstorm148.heyum.delivery.getInstalledQuickCommerceApps
import com.pgstorm148.heyum.delivery.grocerySearchUrl
import com.pgstorm148.heyum.delivery.logoResource
import com.pgstorm148.heyum.delivery.openDeliveryApp
import com.pgstorm148.heyum.model.Dish
import com.pgstorm148.heyum.ui.theme.AccentColor
import com.pgstorm148.heyum.ui.theme.LightGray
import com.pgstorm148.heyum.ui.theme.PrimaryColor
import com.pgstorm148.heyum.ui.theme.TextColor
import com.pgstorm148.heyum.ui.theme.TextMutedColor
import org.jetbrains.compose.resources.painterResource

// Shown when none of the quick-commerce apps are installed: same four options as before, routed
// through openDeliveryApp, which falls back to each app's website when the app itself is absent.
private val FALLBACK_QUICK_COMMERCE_APPS = listOf(
    DeliveryApp(id = "blinkit", displayName = "Blinkit", webFallbackUrl = "https://blinkit.com", category = DeliveryCategory.QUICK_COMMERCE, emoji = "⚡"),
    DeliveryApp(id = "zepto", displayName = "Zepto", webFallbackUrl = "https://www.zeptonow.com", category = DeliveryCategory.QUICK_COMMERCE, emoji = "⚡"),
    DeliveryApp(id = "instamart", displayName = "Instamart", webFallbackUrl = "https://www.swiggy.com/instamart", category = DeliveryCategory.QUICK_COMMERCE, emoji = "🛒"),
    DeliveryApp(id = "bigbasket", displayName = "BigBasket", webFallbackUrl = "https://www.bigbasket.com", category = DeliveryCategory.QUICK_COMMERCE, emoji = "🥦")
)

// bigbasket's site isn't app-linkable for search (see grocerySearchUrl), but its web search is
// still the most useful landing page when the app isn't installed.
private fun searchUrlFor(app: DeliveryApp, item: String): String? =
    app.grocerySearchUrl(item)
        ?: if (app.id == "bigbasket") "https://www.bigbasket.com/ps/?q=${item.trim().replace(" ", "+")}" else null

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
    val quickCommerceApps = remember { getInstalledQuickCommerceApps().ifEmpty { FALLBACK_QUICK_COMMERCE_APPS } }
    var selectedApp by remember { mutableStateOf(quickCommerceApps.first()) }
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                quickCommerceApps.forEach { app ->
                    val isSelected = selectedApp.id == app.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryColor else LightGray.copy(alpha = 0.5f))
                            .clickable { selectedApp = app }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val logoRes = app.logoResource()
                        if (logoRes != null) {
                            Image(
                                painter = painterResource(logoRes),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                            )
                        } else {
                            Text(text = app.emoji, fontSize = 14.sp)
                        }
                        Text(
                            text = app.displayName,
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
                                    try {
                                        openDeliveryApp(selectedApp, searchUrlFor(selectedApp, item))
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
