package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.CarPhotoDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CarPhotosApi {
    @Multipart
    @POST("car-photos/{carId}")
    suspend fun createCarPhoto(
        @Path("carId") carId: UInt,
        @Part file: MultipartBody.Part
    ): Response<CarPhotoDto>

    @GET("car-photos/{photoId}")
    suspend fun getCarPhotoById(
        @Path("photoId") photoId: UInt
    ): Response<ResponseBody>
}

