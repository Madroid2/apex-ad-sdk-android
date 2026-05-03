package com.velora.app.data.repository

import com.velora.app.data.model.CartItem
import com.velora.app.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-source-of-truth for cart state.
 *
 * @Singleton ensures HomeViewModel (cart badge) and CartViewModel (cart screen)
 * both observe the same [StateFlow] — no event bus, no shared preferences, no
 * database needed for the portfolio demo.
 */
@Singleton
class CartRepository @Inject constructor() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    val itemCount: Flow<Int> = _items.map { list -> list.sumOf { it.quantity } }

    val subtotal: Flow<Double> = _items.map { list -> list.sumOf { it.subtotal } }

    fun addItem(product: Product, size: String = "") {
        val current = _items.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == product.id && it.selectedSize == size }
        if (idx >= 0) {
            current[idx] = current[idx].copy(quantity = current[idx].quantity + 1)
        } else {
            current.add(CartItem(product, 1, size))
        }
        _items.value = current
    }

    fun removeItem(productId: String) {
        _items.value = _items.value.filter { it.product.id != productId }
    }

    fun incrementQuantity(productId: String) {
        _items.value = _items.value.map { item ->
            if (item.product.id == productId) item.copy(quantity = item.quantity + 1) else item
        }
    }

    fun decrementQuantity(productId: String) {
        _items.value = _items.value.mapNotNull { item ->
            when {
                item.product.id != productId -> item
                item.quantity > 1 -> item.copy(quantity = item.quantity - 1)
                else -> null
            }
        }
    }

    fun applyDiscount(percent: Int) {
        // In production: store a discount code object and factor it into subtotal.
        // For the demo: mark every item with a discounted price.
        _items.value = _items.value.map { item ->
            val discounted = item.product.price * (1.0 - percent / 100.0)
            item.copy(product = item.product.copy(price = discounted))
        }
    }

    fun contains(productId: String): Boolean =
        _items.value.any { it.product.id == productId }
}
