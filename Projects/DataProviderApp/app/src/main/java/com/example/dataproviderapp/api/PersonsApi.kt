package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.requests.SignUpRequest
import com.example.dataproviderapp.dto.requests.UpdatePersonInfoRequest
import com.example.dataproviderapp.dto.responses.PersonDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PersonsApi {
    @GET("Persons")
    suspend fun getPersonById(): Response<PersonDto>

    @POST("Persons")
    suspend fun signUp(@Body body: SignUpRequest): Response<PersonDto>

    @PUT("Persons")
    suspend fun updatePersonInfo(@Body body: UpdatePersonInfoRequest): Response<Unit>

    @DELETE("Persons/{personId}")
    suspend fun deletePersonById(@Path("personId") personId: UInt): Response<Unit>
}

