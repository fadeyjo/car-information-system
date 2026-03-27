package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.CarDrivesApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.CarBrandDto
import com.example.dataproviderapp.dto.responses.CarDriveDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object CarDrivesRepository {
    private val api: CarDrivesApi by lazy {
        RetrofitClient.auth_retrofit.create(CarDrivesApi::class.java)
    }

    suspend fun getAllDrives(): ApiResult<List<CarDriveDto>> {
        return ApiHandler.handleResponse(api.getAllCarDrives())
    }
}