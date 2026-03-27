package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.CarBrandsApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.CarBrandDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object CarBrandsRepository {
    private val api: CarBrandsApi by lazy {
        RetrofitClient.auth_retrofit.create(CarBrandsApi::class.java)
    }

    suspend fun getAllBrands(): ApiResult<List<CarBrandDto>> {
        return ApiHandler.handleResponse(api.getAllCarBrands())
    }
}