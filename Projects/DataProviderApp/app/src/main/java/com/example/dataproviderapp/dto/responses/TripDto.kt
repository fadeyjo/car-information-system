package com.example.dataproviderapp.dto.responses

import java.time.LocalDateTime

data class TripDto(
    val tripId: ULong,
    val startDatetime: LocalDateTime,
    val endDatetime: LocalDateTime?,
    val deviceId: UInt,
    val car: CarDto,
)
