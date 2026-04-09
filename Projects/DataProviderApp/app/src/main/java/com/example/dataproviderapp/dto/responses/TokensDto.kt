package com.example.dataproviderapp.dto.responses

data class TokensDto(
    val person: PersonDto,
    val accessToken: String,
    val refreshToken: String
)
