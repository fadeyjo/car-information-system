package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.FuelTypesApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.FuelTypeDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object FuelTypesRepository {
    private val api: FuelTypesApi by lazy {
        RetrofitClient.auth_retrofit.create(FuelTypesApi::class.java)
    }

    suspend fun getAllBodies(): ApiResult<List<FuelTypeDto>> {
        return ApiHandler.handleResponse(api.getAllFuelTypes())
    }
}