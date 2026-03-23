package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.AvatarDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface AvatarsApi {
    @Multipart
    @POST("Avatars")
    suspend fun createAvatar(
        @Part file: MultipartBody.Part
    ): Response<AvatarDto>

    @GET("Avatars/{avatarId}")
    suspend fun getAvatarById(
        @Path("avatarId") avatarId: UInt
    ): Response<ResponseBody>
}

