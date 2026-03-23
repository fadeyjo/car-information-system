package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.TripsApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.EndTripRequest
import com.example.dataproviderapp.dto.requests.StartTripRequest
import com.example.dataproviderapp.dto.responses.TripDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object TripsRepository {
    private val api: TripsApi by lazy {
        RetrofitClient.auth_retrofit.create(TripsApi::class.java)
    }

    suspend fun startTrip(body: StartTripRequest): ApiResult<TripDto> {
        return ApiHandler.handleResponse(api.startTrip(body))
    }

    suspend fun getTripById(tripId: ULong): ApiResult<TripDto> {
        return ApiHandler.handleResponse(api.getTripById(tripId))
    }

    suspend fun endTrip(body: EndTripRequest): ApiResult<Unit> {
        return ApiHandler.handleResponse(api.endTrip(body))
    }
}

