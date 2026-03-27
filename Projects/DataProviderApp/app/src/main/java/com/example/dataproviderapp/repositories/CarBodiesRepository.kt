package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.CarBodiesApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.CarBodyDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object CarBodiesRepository {
    private val api: CarBodiesApi by lazy {
        RetrofitClient.auth_retrofit.create(CarBodiesApi::class.java)
    }

    suspend fun getAllBodies(): ApiResult<List<CarBodyDto>> {
        return ApiHandler.handleResponse(api.getAllCarBodies())
    }
}