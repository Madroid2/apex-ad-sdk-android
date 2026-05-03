package com.velora.app.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velora.app.data.model.CartItem
import com.velora.app.data.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CartUiModel(
    val items: List<CartItem>,
    val subtotal: Double,
    val discountPercent: Int = 0,
    val total: Double = subtotal * (1.0 - discountPercent / 100.0),
    val isDiscountApplied: Boolean = discountPercent > 0,
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
) : ViewModel() {

    val uiState: StateFlow<CartUiModel> = combine(
        cartRepository.items,
        cartRepository.subtotal,
    ) { items, subtotal ->
        CartUiModel(items = items, subtotal = subtotal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiModel(items = emptyList(), subtotal = 0.0),
    )

    fun removeItem(productId: String) = cartRepository.removeItem(productId)

    fun increment(productId: String) = cartRepository.incrementQuantity(productId)

    fun decrement(productId: String) = cartRepository.decrementQuantity(productId)

    /** Called after the rewarded video completes successfully. */
    fun applyRewardedDiscount() = cartRepository.applyDiscount(20)
}
