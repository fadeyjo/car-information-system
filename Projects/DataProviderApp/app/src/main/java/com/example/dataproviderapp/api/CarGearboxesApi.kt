package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.CarGearboxDto
import retrofit2.Response
import retrofit2.http.GET

interface CarGearboxesApi {
    @GET("car-gearboxes")
    suspend fun getAllCarGearboxes(): Response<List<CarGearboxDto>>
}