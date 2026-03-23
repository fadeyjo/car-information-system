package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.requests.EndTripRequest
import com.example.dataproviderapp.dto.requests.StartTripRequest
import com.example.dataproviderapp.dto.responses.TripDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

interface TripsApi {
    @POST("Trips/start")
    suspend fun startTrip(@Body body: StartTripRequest): Response<TripDto>

    @GET("Trips/{tripId}")
    suspend fun getTripById(@Path("tripId") tripId: ULong): Response<TripDto>

    @PUT("Trips/end")
    suspend fun endTrip(@Body body: EndTripRequest): Response<Unit>
}

