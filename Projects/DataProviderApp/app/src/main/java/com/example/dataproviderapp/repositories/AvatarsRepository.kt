package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.AvatarsApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.AvatarDto
import com.example.dataproviderapp.retrofit.RetrofitClient
import okhttp3.MultipartBody

object AvatarsRepository {
    private val api: AvatarsApi by lazy {
        RetrofitClient.auth_retrofit.create(AvatarsApi::class.java)
    }

    suspend fun createAvatar(file: MultipartBody.Part): ApiResult<AvatarDto> {
        return ApiHandler.handleResponse(api.createAvatar(file))
    }

    suspend fun getAvatarBytes(avatarId: UInt): ApiResult<ByteArray> {
        return ApiHandler.handleResponseByteArray(api.getAvatarById(avatarId))
    }
}

