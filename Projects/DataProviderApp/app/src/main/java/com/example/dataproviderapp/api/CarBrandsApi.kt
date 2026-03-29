package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.CarBrandDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CarBrandsApi {
    @GET("car-brands")
    suspend fun getAllCarBrands(): Response<List<CarBrandDto>>

    @GET("car-brands/{text}")
    suspend fun getAllCarBrandsByText(@Path("text") text: String): Response<List<CarBrandDto>>
}