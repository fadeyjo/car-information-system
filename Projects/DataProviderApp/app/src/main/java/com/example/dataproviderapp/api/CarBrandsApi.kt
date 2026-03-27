package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.CarBrandDto
import retrofit2.Response
import retrofit2.http.GET

interface CarBrandsApi {
    @GET("car-brands")
    suspend fun getAllCarBrands(): Response<List<CarBrandDto>>
}