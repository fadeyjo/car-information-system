package com.example.dataproviderapp.dto.requests

import java.time.LocalDate

data class EndTripRequest(
    val tripId: ULong,
    val endDatetime: LocalDate
)
