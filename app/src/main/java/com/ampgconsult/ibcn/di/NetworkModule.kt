package com.ampgconsult.ibcn.di

import android.os.Build
import com.ampgconsult.ibcn.data.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // PART 8 — EMULATOR CHECK
        val isEmulator = Build.FINGERPRINT.contains("generic") || 
                         Build.FINGERPRINT.contains("unknown") || 
                         Build.MODEL.contains("google_sdk") || 
                         Build.MODEL.contains("Emulator") || 
                         Build.MODEL.contains("Android SDK built for x86")
        
        val baseUrl = if (isEmulator) {
            "http://10.0.2.2:8000/"
        } else {
            val hostIp = getHostIpGuess()
            "http://$hostIp:8000/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getHostIpGuess(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val ip = address.hostAddress
                        if (ip != null) {
                            val subnet = ip.substringBeforeLast(".")
                            return "$subnet.1" 
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return "10.0.2.2"
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
