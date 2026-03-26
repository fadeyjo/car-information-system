package com.example.dataproviderapp.dto.responses

import java.time.LocalDate

data class PersonDto(
    val personId: UInt,
    val email: String,
    val phone: String,
    val lastName: String,
    val firstName: String,
    val patronymic: String?,
    val birth: LocalDate,
    val driveLicense: String?,
    val roleId: UByte,
    val avatarId: UInt
)
