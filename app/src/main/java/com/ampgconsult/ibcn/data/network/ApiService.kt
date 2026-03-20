package com.ampgconsult.ibcn.data.network

import com.ampgconsult.ibcn.data.models.Project
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path

interface ApiService {
    @GET("projects")
    suspend fun getProjects(): List<Project>

    @POST("projects")
    suspend fun createProject(@Body project: Project): Project

    @GET("projects/{id}")
    suspend fun getProject(@Path("id") id: String): Project
}
