package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.FuelTypeDto
import retrofit2.Response
import retrofit2.http.GET

interface FuelTypesApi {
    @GET("fuel-types")
    suspend fun getAllFuelTypes(): Response<List<FuelTypeDto>>
}