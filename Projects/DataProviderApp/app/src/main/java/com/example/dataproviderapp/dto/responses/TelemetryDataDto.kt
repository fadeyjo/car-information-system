package com.example.dataproviderapp.dto.responses

import java.time.LocalDateTime

data class TelemetryDataDto(
    val recId: ULong,
    val recDatetime: LocalDateTime,
    val serviceId: UByte,
    val pid: UShort,
    val ecuIdBase64: String,
    val responseDlc: UByte,
    val responseBase64: String?,
    val tripId: ULong
)
