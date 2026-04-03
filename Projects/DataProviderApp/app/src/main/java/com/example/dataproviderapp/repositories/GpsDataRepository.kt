package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.GpsDataApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.CreateGpsDataRequest
import com.example.dataproviderapp.dto.responses.GpsDataDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object GpsDataRepository {
    private val api: GpsDataApi by lazy {
        RetrofitClient.auth_retrofit.create(GpsDataApi::class.java)
    }

    suspend fun createGpsData(body: CreateGpsDataRequest): ApiResult<GpsDataDto> {
        return ApiHandler.handleResponse(api.createGpsData(body))
    }

    suspend fun getGpsDataById(recordId: ULong): ApiResult<GpsDataDto> {
        return ApiHandler.handleResponse(api.getGpsDataById(recordId))
    }
}

