package com.example.dataproviderapp.apiutils

sealed class ApiResult<out T> {
    data class Success<T>(val data: T?) : ApiResult<T>()

    data class Error(
        val code: Int,
        val error: String
    ) : ApiResult<Nothing>()

    data class ValidationError(
        val errors: List<Map<String, String>>
    ) : ApiResult<Nothing>()

    object NetworkError : ApiResult<Nothing>()

    object UnknownError : ApiResult<Nothing>()
}