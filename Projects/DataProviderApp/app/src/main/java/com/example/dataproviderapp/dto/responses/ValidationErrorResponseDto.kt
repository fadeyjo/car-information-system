package com.example.dataproviderapp.dto.responses

data class ValidationErrorResponseDto(
    val status: Int,
    val errors: Map<String, List<String>>
)
