package com.example.dataproviderapp.repositories

import com.example.dataproviderapp.api.GpsDataApi
import com.example.dataproviderapp.api.OBDIIPIDSApi
import com.example.dataproviderapp.apiutils.ApiHandler
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.responses.PidsDetailDto
import com.example.dataproviderapp.retrofit.RetrofitClient

object OBDIIPIDSRepository {

    private val api: OBDIIPIDSApi by lazy {
        RetrofitClient.auth_retrofit.create(OBDIIPIDSApi::class.java)
    }

    suspend fun getCurrentDataSupportedPids(pids: Long): ApiResult<PidsDetailDto> {
        return ApiHandler.handleResponse(api.getCurrentDataSupportedPids(pids))
    }
}