package com.example.dataproviderapp.dto.requests

data class CreateTelemetryDataRequest(
    val recDatetime: String,
    val serviceId: UByte,
    val pid: UShort,
    val ecuId: String,
    val responseDlc: UByte,
    val response: String?,
    val tripId: ULong
)
