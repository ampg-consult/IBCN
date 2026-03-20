package com.ampgconsult.ibcn.di

import com.ampgconsult.ibcn.data.ai.AIProvider
import com.ampgconsult.ibcn.data.ai.LocalAIProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindAIProvider(
        localAIProviderImpl: LocalAIProviderImpl
    ): AIProvider
}
