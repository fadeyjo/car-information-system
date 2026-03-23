package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.requests.CreateTelemetryDataRequest
import com.example.dataproviderapp.dto.responses.TelemetryDataDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TelemetryDataApi {
    @POST("telemetry-data")
    suspend fun createTelemetryData(
        @Body body: CreateTelemetryDataRequest
    ): Response<TelemetryDataDto>

    @GET("telemetry-data/{recordId}")
    suspend fun getTelemetryDataById(@Path("recordId") recordId: ULong): Response<TelemetryDataDto>
}

