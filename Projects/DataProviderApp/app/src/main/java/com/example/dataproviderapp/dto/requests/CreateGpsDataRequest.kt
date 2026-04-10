package com.example.dataproviderapp.dto.requests

import java.time.LocalDateTime

data class CreateGpsDataRequest(
    val recDatetime: String,
    val tripId: ULong,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val accuracyM: Float?,
    val speedKmh: Int?,
    val bearingDeg: Int?
)
