package com.example.dataproviderapp.dto.requests

import java.time.LocalDate

data class UpdatePersonInfoRequest(
    val email: String,
    val phone: String,
    val lastName: String,
    val firstName: String,
    val patronymic: String?,
    val birth: LocalDate,
    val driveLicense: String?
)
