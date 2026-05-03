package com.velora.app.data.repository

import com.velora.app.data.model.Category
import com.velora.app.data.model.FeedItem
import com.velora.app.data.model.HeroBanner
import com.velora.app.data.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getHeroBanners(): Flow<List<HeroBanner>>
    fun getProducts(category: Category): Flow<List<Product>>
    fun getFeedItems(category: Category): Flow<List<FeedItem>>
    fun getProduct(id: String): Flow<Product?>
    suspend fun toggleFavorite(productId: String)
}
