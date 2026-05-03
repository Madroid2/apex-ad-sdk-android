package com.velora.app.feature.product

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.velora.app.core.design.ErrorRed
import com.velora.app.core.design.StarYellow
import com.velora.app.core.design.White
import com.velora.app.core.design.White40
import com.velora.app.data.model.Product
import com.velora.app.data.model.UiState

@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onGoToCart: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSize by viewModel.selectedSize.collectAsStateWithLifecycle()
    val isInCart by viewModel.isInCart.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize()) {
            Text("Loading...", Modifier.align(Alignment.Center))
        }
        is UiState.Error   -> Box(Modifier.fillMaxSize()) {
            Text(state.message, Modifier.align(Alignment.Center))
        }
        is UiState.Success -> ProductDetailContent(
            product = state.data,
            selectedSize = selectedSize,
            isInCart = isInCart,
            onBack = onBack,
            onGoToCart = onGoToCart,
            onSelectSize = viewModel::selectSize,
            onAddToCart = viewModel::addToCart,
            onToggleFavorite = viewModel::toggleFavorite,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailContent(
    product: Product,
    selectedSize: String?,
    isInCart: Boolean,
    onBack: () -> Unit,
    onGoToCart: () -> Unit,
    onSelectSize: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val scrollState = rememberLazyListState()

    // Parallax: image translates at 40% scroll speed
    val imageParallaxOffset by remember {
        derivedStateOf { scrollState.firstVisibleItemScrollOffset * 0.4f }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            // ── Hero image with parallax ──────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(520.dp)
                            .graphicsLayer { translationY = -imageParallaxOffset },
                    )
                    // Bottom gradient fade into surface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        MaterialTheme.colorScheme.surface,
                                    ),
                                ),
                            ),
                    )
                }
            }

            // ── Product info ──────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = product.brand.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    // Rating row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (i < product.rating.toInt()) StarYellow
                                       else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${product.rating} · ${product.reviewCount} reviews",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Price
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$${product.price.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        product.originalPrice?.let { orig ->
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "$${orig.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    // Description
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Size selector ─────────────────────────────────────────────
            if (product.sizes.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Select Size",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (selectedSize != null) {
                                Text(
                                    text = selectedSize,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            product.sizes.forEach { size ->
                                val isSelected = size == selectedSize
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outline,
                                            shape = MaterialTheme.shapes.small,
                                        )
                                        .clickable { onSelectSize(size) },
                                ) {
                                    Text(
                                        text = size,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Floating back + favourite row ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = White40,
                modifier = Modifier.size(40.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = White)
                }
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = White40,
                modifier = Modifier.size(40.dp),
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Filled.Favorite
                                      else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favourite",
                        tint = if (product.isFavorite) ErrorRed else White,
                    )
                }
            }
        }

        // ── Add to Cart / Go to Cart — morphing button ────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            AnimatedContent(
                targetState = isInCart,
                transitionSpec = {
                    (scaleIn(initialScale = 0.9f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.9f) + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "cart_button",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) { inCart ->
                if (!inCart) {
                    Button(
                        onClick = { onAddToCart(product) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(Icons.Outlined.ShoppingBag, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add to Cart", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = onGoToCart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View in Cart", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
