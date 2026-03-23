package com.example.dataproviderapp.dto.requests

import java.time.LocalDateTime

data class CreateGpsDataRequest(
    val recDatetime: LocalDateTime,
    val tripId: ULong,
    val latitudeDeg: Float,
    val longitudeDeg: Float,
    val accuracyM: Float?,
    val speedKmh: UInt?,
    val bearingDeg: UShort?
)
