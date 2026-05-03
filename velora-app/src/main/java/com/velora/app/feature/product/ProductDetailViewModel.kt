package com.velora.app.feature.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velora.app.data.model.Product
import com.velora.app.data.model.UiState
import com.velora.app.data.repository.CartRepository
import com.velora.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val _productId = MutableStateFlow<String?>(null)

    private val _selectedSize = MutableStateFlow<String?>(null)
    val selectedSize: StateFlow<String?> = _selectedSize.asStateFlow()

    val uiState: StateFlow<UiState<Product>> = _productId
        .map { id ->
            if (id == null) return@map UiState.Loading
            val product = productRepository.getProduct(id)
                .stateIn(viewModelScope, SharingStarted.Eagerly, null).value
            if (product != null) UiState.Success(product) else UiState.Error("Product not found")
        }
        .onStart { emit(UiState.Loading) }
        .catch { e -> emit(UiState.Error(e.message ?: "Error loading product")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    val isInCart: StateFlow<Boolean> = _productId
        .map { id -> id != null && cartRepository.contains(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun loadProduct(productId: String) {
        _productId.value = productId
    }

    fun selectSize(size: String) {
        _selectedSize.value = size
    }

    fun addToCart(product: Product) {
        cartRepository.addItem(product, _selectedSize.value ?: "")
    }

    fun toggleFavorite() {
        val id = _productId.value ?: return
        viewModelScope.launch { productRepository.toggleFavorite(id) }
    }
}
