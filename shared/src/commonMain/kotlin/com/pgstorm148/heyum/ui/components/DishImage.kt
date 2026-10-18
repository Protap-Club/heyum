package com.pgstorm148.heyum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.pgstorm148.heyum.model.Cuisine
import com.pgstorm148.heyum.model.Dish
import com.pgstorm148.heyum.model.getDefaultDishPool
import org.jetbrains.compose.resources.ExperimentalResourceApi
import heyum.shared.generated.resources.Res

/** Firebase Storage bucket holding the full-resolution dish photos (see docs/DISH_IMAGES.md). */
private const val STORAGE_BUCKET = "heyum-de1f9.firebasestorage.app"

/**
 * Public download URL for `dishes/<id>.jpg` in Firebase Storage.
 *
 * Deriving this from the dish id means uploading a photo is the only step — there's no need to
 * write 150 imageUrl values into Firestore or the dish pool. A dish that *does* carry an explicit
 * [Dish.imageUrl] still wins, so one-off overrides remain possible.
 */
fun storageUrlForDish(dishId: String): String =
    "https://firebasestorage.googleapis.com/v0/b/$STORAGE_BUCKET/o/dishes%2F$dishId.jpg?alt=media"

private val knownDishIds: Set<String> by lazy { getDefaultDishPool().map { it.id }.toSet() }
private val dishIdByName: Map<String, String> by lazy {
    getDefaultDishPool().associate { it.name.trim().lowercase() to it.id }
}

/**
 * The id whose photo files this dish should use. Dishes in Firestore can carry ids that are not
 * in the built-in catalog (older seeds, duplicates), so when the id is unknown we match on the
 * dish name instead — otherwise a dish like "Baingan Bharta" would miss its photo.
 */
private fun photoIdFor(dish: Dish): String {
    if (dish.id in knownDishIds) return dish.id
    val name = dish.name.trim().lowercase()
    dishIdByName[name]?.let { return it }
    // Looser match for renamed variants ("Pad Thai" vs "Classic Pad Thai"): accept it only when
    // exactly one catalog dish fits, so a vague name can never pick the wrong photo.
    if (name.length >= 5) {
        val candidates = dishIdByName.filterKeys { it.contains(name) || name.contains(it) }
        if (candidates.size == 1) return candidates.values.first()
    }
    return dish.id
}

/**
 * Dish photo with a three-tier fallback, so the card always renders something sensible:
 *
 *  1. The copy bundled in the app at `composeResources/files/dishes/<dish id>.jpg` — local, so it
 *     appears immediately and works offline, at full quality. It is read with Compose's own
 *     resource reader (not Coil): Coil's loader would sit on "loading" forever for these files.
 *  2. The hosted photo in Firebase Storage, loaded with Coil — for dishes added after this app
 *     version shipped, which have no bundled copy. An explicit [Dish.imageUrl] takes priority and
 *     is tried first, so one-off overrides remain possible.
 *  3. The original cuisine gradient plus emoji, for dishes that have no photo at all yet.
 *
 * The bundled copy is addressed by path rather than by a generated accessor, so a new dish only
 * needs a file dropped in — no code change and no 150-entry lookup table.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun DishImage(
    dish: Dish,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 64.sp
) {
    val photoId = remember(dish.id, dish.name) { photoIdFor(dish) }
    val explicitUrl = dish.imageUrl.takeIf { it.isNotBlank() }
    val hostedUrl = explicitUrl ?: photoId.takeIf { it.isNotBlank() }?.let { storageUrlForDish(it) }

    // null = still reading, false = no bundled copy (or unreadable)
    var bundled by remember(photoId) { mutableStateOf<ImageBitmap?>(null) }
    var bundledMissing by remember(photoId) { mutableStateOf(false) }
    LaunchedEffect(photoId) {
        if (photoId.isBlank()) {
            bundledMissing = true
            return@LaunchedEffect
        }
        val bmp = runCatching {
            val bytes = Res.readBytes("files/dishes/$photoId.jpg")
            withContext(Dispatchers.Default) { bytes.decodeToImageBitmap() }
        }.getOrNull()
        if (bmp != null) bundled = bmp else bundledMissing = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val local = bundled
        when {
            // An explicit imageUrl wins over the bundled copy.
            explicitUrl == null && local != null -> Image(
                bitmap = local,
                contentDescription = dish.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            explicitUrl == null && !bundledMissing -> DishGradientFallback(dish, emojiSize)   // still reading
            hostedUrl == null -> DishGradientFallback(dish, emojiSize)
            else -> HostedDishImage(dish, hostedUrl, emojiSize)
        }
    }
}

/** Coil-loaded hosted photo; shows the gradient while loading and if it fails. */
@Composable
private fun HostedDishImage(dish: Dish, url: String, emojiSize: TextUnit) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = dish.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    ) {
        when (painter.state.value) {
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Error -> DishGradientFallback(dish, emojiSize)
            else -> SubcomposeAsyncImageContent()
        }
    }
}

/** The pre-image look: a cuisine-tinted gradient with a representative emoji. */
@Composable
private fun DishGradientFallback(dish: Dish, emojiSize: TextUnit) {
    val gradient = remember(dish.id) { Brush.verticalGradient(gradientColorsFor(dish)) }
    Box(
        modifier = Modifier.fillMaxSize().background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emojiFor(dish.cuisine), fontSize = emojiSize, textAlign = TextAlign.Center)
    }
}

private fun gradientColorsFor(dish: Dish): List<Color> = when (dish.cuisine) {
    Cuisine.INDIAN -> listOf(Color(0xFFFF9A3C), Color(0xFFD84315))
    Cuisine.CHINESE -> listOf(Color(0xFFE05C5C), Color(0xFF8E1F1F))
    Cuisine.ITALIAN -> listOf(Color(0xFF7CB342), Color(0xFF33691E))
    Cuisine.MEXICAN -> listOf(Color(0xFFFFB300), Color(0xFFE65100))
    Cuisine.JAPANESE -> listOf(Color(0xFFEC7A9B), Color(0xFFAD1457))
    Cuisine.THAI -> listOf(Color(0xFF4DB6AC), Color(0xFF00695C))
    Cuisine.MEDITERRANEAN -> listOf(Color(0xFF4FC3F7), Color(0xFF0277BD))
    Cuisine.STREET_FOOD -> listOf(Color(0xFFFFA726), Color(0xFFBF360C))
    Cuisine.HEALTHY -> listOf(Color(0xFF9CCC65), Color(0xFF2E7D32))
    Cuisine.FAST_FOOD -> listOf(Color(0xFFFF7043), Color(0xFFBF360C))
    Cuisine.COMFORT -> listOf(Color(0xFFA1887F), Color(0xFF4E342E))
}

private fun emojiFor(cuisine: Cuisine): String = when (cuisine) {
    Cuisine.INDIAN -> "🍛"
    Cuisine.CHINESE -> "🥢"
    Cuisine.ITALIAN -> "🍝"
    Cuisine.MEXICAN -> "🌮"
    Cuisine.JAPANESE -> "🍱"
    Cuisine.THAI -> "🍜"
    Cuisine.MEDITERRANEAN -> "🥙"
    Cuisine.STREET_FOOD -> "🥘"
    Cuisine.HEALTHY -> "🥗"
    Cuisine.FAST_FOOD -> "🍔"
    Cuisine.COMFORT -> "🍲"
}
