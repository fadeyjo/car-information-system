package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.CarsApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.CreateCarRequest
import com.example.dataproviderapp.dto.requests.UpdateCarInfoRequest
import com.example.dataproviderapp.dto.responses.CarDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object CarsRepository {
    private val api: CarsApi by lazy {
        RetrofitClient.auth_retrofit.create(CarsApi::class.java)
    }

    suspend fun createCar(body: CreateCarRequest): ApiResult<CarDto> {
        return ApiHandler.handleResponse(api.createCar(body))
    }

    suspend fun updateCarInfo(carId: UInt, body: UpdateCarInfoRequest): ApiResult<Unit> {
        return ApiHandler.handleResponse(api.updateCarInfo(carId, body))
    }

    suspend fun getCarById(carId: UInt): ApiResult<CarDto> {
        return ApiHandler.handleResponse(api.getCarById(carId))
    }

    suspend fun getCarsByPersonId(): ApiResult<List<CarDto>> {
        return ApiHandler.handleResponse(api.getCarsByPersonId())
    }
}

