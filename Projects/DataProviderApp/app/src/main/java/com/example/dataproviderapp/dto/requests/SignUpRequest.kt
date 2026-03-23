package com.example.dataproviderapp.dto.requests

import java.time.LocalDate

data class SignUpRequest(
    val email: String,
    val phone: String,
    val lastName: String,
    val firstName: String,
    val patronymic: String?,
    val birth: LocalDate,
    val password: String,
    val driveLicense: String?,
    val roleId: UByte
)
