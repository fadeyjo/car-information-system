package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.CarGearboxesApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.CarGearboxDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object CarGearboxesRepository {
    private val api: CarGearboxesApi by lazy {
        RetrofitClient.auth_retrofit.create(CarGearboxesApi::class.java)
    }

    suspend fun getAllGearboxes(): ApiResult<List<CarGearboxDto>> {
        return ApiHandler.handleResponse(api.getAllCarGearboxes())
    }
}