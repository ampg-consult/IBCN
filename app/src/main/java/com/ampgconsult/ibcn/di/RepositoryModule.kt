package com.ampgconsult.ibcn.di

import com.ampgconsult.ibcn.data.local.dao.ProjectDao
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.network.ApiService
import com.ampgconsult.ibcn.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        apiService: ApiService
    ): ProjectRepository = ProjectRepository(projectDao, apiService)

    @Provides
    @Singleton
    fun provideSaaSService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        paymentManager: PaymentManager
    ): SaaSService = SaaSService(firestore, auth, paymentManager)

    @Provides
    @Singleton
    fun provideLegalService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        aiService: AIService
    ): LegalService = LegalService(firestore, auth, aiService)

    @Provides
    @Singleton
    fun provideInvestorService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        aiService: AIService,
        legalService: LegalService,
        paymentManager: PaymentManager
    ): InvestorService = InvestorService(firestore, auth, aiService, legalService, paymentManager)

    @Provides
    @Singleton
    fun provideStartupIncubatorService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        aiService: AIService
    ): StartupIncubatorService = StartupIncubatorService(firestore, auth, aiService)

    @Provides
    @Singleton
    fun provideGrowthService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        aiService: AIService
    ): GrowthService = GrowthService(firestore, auth, aiService)
}
