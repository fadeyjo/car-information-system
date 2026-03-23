package com.example.dataproviderapp.apiutils

import com.example.dataproviderapp.dto.responses.ErrorDto
import com.example.dataproviderapp.dto.responses.ValidationErrorItemDto
import com.example.dataproviderapp.dto.responses.ValidationErrorResponseDto
import com.google.gson.Gson
import okio.IOException
import okhttp3.ResponseBody
import retrofit2.Response

object ApiHandler {

    fun <T> handleResponse(response: Response<T>): ApiResult<T> {
        return try {
            if (response.isSuccessful) {
                ApiResult.Success(response.body())
            } else {
                val errorBody = response.errorBody()?.string()

                if (errorBody.isNullOrEmpty()) {
                    return ApiResult.UnknownError
                }

                val gson = Gson()

                if (errorBody.trim().startsWith("[")) {
                    val list = gson.fromJson(
                        errorBody,
                        Array<ValidationErrorItemDto>::class.java
                    ).toList()

                    val mapped = list.map {
                        mapOf(it.propertyName to it.errorMessage)
                    }

                    return ApiResult.ValidationError(mapped)
                }

                try {
                    val dto = gson.fromJson(
                        errorBody,
                        ValidationErrorResponseDto::class.java
                    )

                    if (dto.errors.isNotEmpty()) {
                        val mapped = dto.errors.map { (key, value) ->
                            mapOf(key to (value.firstOrNull() ?: ""))
                        }

                        return ApiResult.ValidationError(mapped)
                    }
                } catch (_: Exception) {}

                try {
                    val dto = gson.fromJson(
                        errorBody,
                        ErrorDto::class.java
                    )

                    return ApiResult.Error(dto.status, dto.title)
                } catch (_: Exception) {}

                ApiResult.UnknownError
            }
        } catch (_: IOException) {
            ApiResult.NetworkError
        } catch (_: Exception) {
            ApiResult.UnknownError
        }
    }

    fun handleResponseByteArray(response: Response<ResponseBody>): ApiResult<ByteArray> {
        return try {
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.bytes())
            } else {
                val errorBody = response.errorBody()?.string()

                if (errorBody.isNullOrEmpty()) {
                    return ApiResult.UnknownError
                }

                val gson = Gson()

                if (errorBody.trim().startsWith("[")) {
                    val list = gson.fromJson(
                        errorBody,
                        Array<ValidationErrorItemDto>::class.java
                    ).toList()

                    val mapped = list.map {
                        mapOf(it.propertyName to it.errorMessage)
                    }

                    return ApiResult.ValidationError(mapped)
                }

                try {
                    val dto = gson.fromJson(
                        errorBody,
                        ValidationErrorResponseDto::class.java
                    )

                    if (dto.errors.isNotEmpty()) {
                        val mapped = dto.errors.map { (key, value) ->
                            mapOf(key to (value.firstOrNull() ?: ""))
                        }

                        return ApiResult.ValidationError(mapped)
                    }
                } catch (_: Exception) {}

                try {
                    val dto = gson.fromJson(
                        errorBody,
                        ErrorDto::class.java
                    )

                    return ApiResult.Error(dto.status, dto.title)
                } catch (_: Exception) {}

                ApiResult.UnknownError
            }
        } catch (_: IOException) {
            ApiResult.NetworkError
        } catch (_: Exception) {
            ApiResult.UnknownError
        }
    }
}