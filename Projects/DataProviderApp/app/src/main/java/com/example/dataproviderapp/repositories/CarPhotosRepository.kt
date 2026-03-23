package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.CarPhotosApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.CarPhotoDto
import com.example.dataproviderapp.retrofit.RetrofitClient
import okhttp3.MultipartBody

object CarPhotosRepository {
    private val api: CarPhotosApi by lazy {
        RetrofitClient.auth_retrofit.create(CarPhotosApi::class.java)
    }

    suspend fun createCarPhoto(carId: UInt, file: MultipartBody.Part): ApiResult<CarPhotoDto> {
        return ApiHandler.handleResponse(api.createCarPhoto(carId, file))
    }

    suspend fun getCarPhotoBytes(photoId: UInt): ApiResult<ByteArray> {
        return ApiHandler.handleResponseByteArray(api.getCarPhotoById(photoId))
    }
}

