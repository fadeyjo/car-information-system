package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.CarBodyDto
import retrofit2.Response
import retrofit2.http.GET

interface CarBodiesApi {
    @GET("car-bodies")
    suspend fun getAllCarBodies(): Response<List<CarBodyDto>>
}