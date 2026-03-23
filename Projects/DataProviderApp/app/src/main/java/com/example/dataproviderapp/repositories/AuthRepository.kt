package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.RefreshTokenApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.RefreshTokensRequest
import com.example.dataproviderapp.dto.requests.SignInRequest
import com.example.dataproviderapp.dto.responses.LogOutDto
import com.example.dataproviderapp.dto.responses.TokensDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object AuthRepository {
    private val publicRefreshApi: RefreshTokenApi by lazy {
        RetrofitClient.public_retrofit.create(RefreshTokenApi::class.java)
    }

    private val authRefreshApi: RefreshTokenApi by lazy {
        RetrofitClient.auth_retrofit.create(RefreshTokenApi::class.java)
    }

    suspend fun logIn(body: SignInRequest): ApiResult<TokensDto> {
        return ApiHandler.handleResponse(publicRefreshApi.logIn(body))
    }

    suspend fun refreshTokens(body: RefreshTokensRequest): ApiResult<TokensDto> {
        return ApiHandler.handleResponse(RetrofitClient.refreshApi.refresh(body))
    }

    suspend fun logOut(): ApiResult<LogOutDto> {
        return ApiHandler.handleResponse(authRefreshApi.logOut())
    }
}

