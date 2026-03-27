package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.CarModelDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CarModelsApi {
    @GET("car-models/{brandName}")
    suspend fun getAllModelsByBrandName(@Path("brandName") brandName: String): Response<List<CarModelDto>>
}