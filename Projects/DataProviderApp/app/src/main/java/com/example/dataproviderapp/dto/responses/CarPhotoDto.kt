package com.example.dataproviderapp.dto.responses

data class CarPhotoDto(
    val photoId: UInt,
    val photoUrl: String,
    val carId: UInt,
    val contentType: String
)
