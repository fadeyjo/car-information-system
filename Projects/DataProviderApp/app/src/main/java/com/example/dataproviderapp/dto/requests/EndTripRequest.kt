package com.example.dataproviderapp.dto.requests

import java.time.LocalDateTime

data class EndTripRequest(
    val tripId: ULong,
    val endDatetime: LocalDateTime
)
