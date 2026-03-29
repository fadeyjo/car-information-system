package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.CarModelsApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.CarModelDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object CarModelsRepository {
    private val api: CarModelsApi by lazy {
        RetrofitClient.auth_retrofit.create(CarModelsApi::class.java)
    }

    suspend fun getAllModelsByBrandName(brandName: String): ApiResult<List<CarModelDto>> {
        return ApiHandler.handleResponse(api.getAllModelsByBrandName(brandName))
    }

    suspend fun getAllBrandsByText(brandModel: String, text: String): ApiResult<List<CarModelDto>> {
        return ApiHandler.handleResponse(api.getAllCarModelsByText(brandModel, text))
    }
}