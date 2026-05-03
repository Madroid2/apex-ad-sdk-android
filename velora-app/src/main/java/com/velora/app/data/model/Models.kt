package com.velora.app.data.model

// ── Domain models ─────────────────────────────────────────────────────────────

data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val description: String,
    val price: Double,
    val originalPrice: Double? = null,
    val category: Category,
    val rating: Float,
    val reviewCount: Int,
    val imageUrl: String,
    val sizes: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val badge: String? = null,
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val selectedSize: String = "",
) {
    val subtotal: Double get() = product.price * quantity
}

data class HeroBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    val backgroundGradientStart: Long,
    val backgroundGradientEnd: Long,
    val imageUrl: String,
    val targetCategory: Category? = null,
)

data class OrderStat(
    val label: String,
    val value: Float,
)

enum class Category(val label: String) {
    ALL("All"),
    BAGS("Bags"),
    KNITWEAR("Knitwear"),
    OUTERWEAR("Outerwear"),
    FOOTWEAR("Footwear"),
    ACCESSORIES("Accessories"),
}

// ── UI state wrapper ──────────────────────────────────────────────────────────

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// ── Feed items (products interleaved with native ads) ─────────────────────────

sealed interface FeedItem {
    data class ProductItem(val product: Product) : FeedItem
    data object NativeAdSlot : FeedItem
}
