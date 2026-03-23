package com.example.dataproviderapp.dto.responses

import java.time.LocalDateTime

data class TripDto(
    val tripId: ULong,
    val startDatetime: LocalDateTime,
    val deviceId: UInt,
    val carId: UInt,
    val endDatetime: LocalDateTime?
)
