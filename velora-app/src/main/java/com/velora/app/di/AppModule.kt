package com.velora.app.di

import com.velora.app.data.repository.FakeProductRepository
import com.velora.app.data.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: FakeProductRepository): ProductRepository
}
