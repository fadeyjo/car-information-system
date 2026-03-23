package com.example.dataproviderapp.dto.requests

import java.time.LocalDateTime

data class CreateTelemetryDataRequest(
    val recDatetime: LocalDateTime,
    val serviceId: UByte,
    val pid: UShort,
    val ecuId: String,
    val responseDlc: UByte,
    val response: String?,
    val tripId: ULong
)
