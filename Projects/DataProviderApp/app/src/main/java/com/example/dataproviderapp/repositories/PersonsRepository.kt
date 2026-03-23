package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.PersonsApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.SignUpRequest
import com.example.dataproviderapp.dto.requests.UpdatePersonInfoRequest
import com.example.dataproviderapp.dto.responses.PersonDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object PersonsRepository {
    private val publicApi: PersonsApi by lazy {
        RetrofitClient.public_retrofit.create(PersonsApi::class.java)
    }

    private val authApi: PersonsApi by lazy {
        RetrofitClient.auth_retrofit.create(PersonsApi::class.java)
    }

    suspend fun signUp(body: SignUpRequest): ApiResult<PersonDto> {
        return ApiHandler.handleResponse(publicApi.signUp(body))
    }

    suspend fun getPersonById(): ApiResult<PersonDto> {
        return ApiHandler.handleResponse(authApi.getPersonById())
    }

    suspend fun updatePersonInfo(body: UpdatePersonInfoRequest): ApiResult<Unit> {
        return ApiHandler.handleResponse(authApi.updatePersonInfo(body))
    }

    suspend fun deletePersonById(personId: UInt): ApiResult<Unit> {
        return ApiHandler.handleResponse(authApi.deletePersonById(personId))
    }
}

