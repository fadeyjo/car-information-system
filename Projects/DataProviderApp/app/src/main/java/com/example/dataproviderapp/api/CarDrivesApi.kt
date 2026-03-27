package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.CarDriveDto
import retrofit2.Response
import retrofit2.http.GET

interface CarDrivesApi {
    @GET("car-drives")
    suspend fun getAllCarDrives(): Response<List<CarDriveDto>>
}