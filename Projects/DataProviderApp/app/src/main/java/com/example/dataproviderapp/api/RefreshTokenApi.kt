package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.requests.SignInRequest
import com.example.dataproviderapp.dto.requests.RefreshTokensRequest
import com.example.dataproviderapp.dto.responses.LogOutDto
import com.example.dataproviderapp.dto.responses.TokensDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshTokenApi {
    @POST("refresh-tokens/login")
    suspend fun logIn(@Body body: SignInRequest): Response<TokensDto>

    @POST("refresh-tokens/refresh")
    suspend fun refresh(@Body body: RefreshTokensRequest): Response<TokensDto>

    @POST("refresh-tokens/logout")
    suspend fun logOut(): Response<LogOutDto>
}