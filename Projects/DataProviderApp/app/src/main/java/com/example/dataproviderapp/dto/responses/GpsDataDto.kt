package com.example.dataproviderapp.dto.responses

import java.time.LocalDateTime

data class GpsDataDto(
    val recId: ULong,
    val recDatetime: LocalDateTime,
    val tripId: ULong,
    val latitudeDeg: Float,
    val longitudeDeg: Float,
    val accuracyM: Float?,
    val speedKmh: UInt?,
    val bearingDeg: UShort?
)
