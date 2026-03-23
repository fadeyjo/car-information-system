package com.example.dataproviderapp.dto.requests

import java.time.LocalDateTime

data class StartTripRequest(
    val startDatetime: LocalDateTime,
    val macAddress: String,
    val carId: UInt
)
