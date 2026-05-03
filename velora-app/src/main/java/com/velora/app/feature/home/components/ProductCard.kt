package com.velora.app.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.velora.app.core.design.ErrorRed
import com.velora.app.core.design.Gold400
import com.velora.app.core.design.StarYellow
import com.velora.app.core.design.White
import com.velora.app.data.model.Product

@Composable
fun ProductCard(
    product: Product,
    onClick: (Product) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick(product) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Column {
            // ── Image ──────────────────────────────────────────────────────
            Box {
                val imageHeight = if (product.id.hashCode() % 3 == 0) 220.dp
                                  else if (product.id.hashCode() % 3 == 1) 180.dp
                                  else 200.dp
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight),
                )

                // Badge (Sale / New / Bestseller)
                product.badge?.let { badge ->
                    val badgeColor = when (badge) {
                        "Sale"       -> ErrorRed
                        "New"        -> MaterialTheme.colorScheme.primary
                        "Bestseller" -> Gold400
                        else         -> MaterialTheme.colorScheme.secondary
                    }
                    Text(
                        text = badge.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = White,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(badgeColor, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }

                // Favourite button
                val favScale by animateFloatAsState(
                    targetValue = if (product.isFavorite) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    label = "fav_scale_${product.id}",
                )
                val favTint by animateColorAsState(
                    targetValue = if (product.isFavorite) ErrorRed else White,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "fav_tint_${product.id}",
                )
                IconButton(
                    onClick = { onFavoriteToggle(product.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .scale(favScale),
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Filled.Favorite
                                      else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (product.isFavorite) "Remove from wishlist"
                                            else "Add to wishlist",
                        tint = favTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // ── Info ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.brand.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                // Star rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "%.1f".format(product.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " (${product.reviewCount})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Price
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${product.price.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    product.originalPrice?.let { orig ->
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$${orig.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
            }
        }
    }
}
