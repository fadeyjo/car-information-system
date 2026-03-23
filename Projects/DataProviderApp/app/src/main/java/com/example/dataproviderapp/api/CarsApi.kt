package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.requests.CreateCarRequest
import com.example.dataproviderapp.dto.requests.UpdateCarInfoRequest
import com.example.dataproviderapp.dto.responses.CarDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT

interface CarsApi {
    @POST("Cars")
    suspend fun createCar(@Body body: CreateCarRequest): Response<CarDto>

    @PUT("Cars/{carId}")
    suspend fun updateCarInfo(
        @Path("carId") carId: UInt,
        @Body body: UpdateCarInfoRequest
    ): Response<Unit>

    @GET("Cars/{carId}")
    suspend fun getCarById(@Path("carId") carId: UInt): Response<CarDto>

    @GET("Cars/my-cars")
    suspend fun getCarsByPersonId(): Response<List<CarDto>>
}

