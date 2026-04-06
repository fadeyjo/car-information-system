package com.example.dataproviderapp.api

import com.example.dataproviderapp.dto.responses.PidsDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OBDIIPIDSApi {
    @GET("obdii-pids/current-data-service/{supportedPidsUint}")
    suspend fun getCurrentDataSupportedPids(@Path("supportedPidsUint") pids: Long): Response<PidsDetailDto>
}