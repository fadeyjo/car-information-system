package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.requests.CreateGpsDataRequest
import com.example.dataproviderapp.dto.responses.GpsDataDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GpsDataApi {
    @POST("gps-data")
    suspend fun createGpsData(@Body body: CreateGpsDataRequest): Response<GpsDataDto>

    @GET("gps-data/{recordId}")
    suspend fun getGpsDataById(@Path("recordId") recordId: ULong): Response<GpsDataDto>
}

