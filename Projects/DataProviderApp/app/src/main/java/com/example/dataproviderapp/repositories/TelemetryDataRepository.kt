package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.TelemetryDataApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.CreateTelemetryDataRequest
import com.example.dataproviderapp.dto.responses.TelemetryDataDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object TelemetryDataRepository {
    private val api: TelemetryDataApi by lazy {
        RetrofitClient.auth_retrofit.create(TelemetryDataApi::class.java)
    }

    suspend fun createTelemetryData(body: CreateTelemetryDataRequest): ApiResult<TelemetryDataDto> {
        return ApiHandler.handleResponse(api.createTelemetryData(body))
    }

    suspend fun getTelemetryDataById(recordId: ULong): ApiResult<TelemetryDataDto> {
        return ApiHandler.handleResponse(api.getTelemetryDataById(recordId))
    }
}

