package com.example.dataproviderapp.dto.responses

data class CarDto(
    val carId: UInt,
    val vinNumber: String,
    val stateNumber: String?,
    val brandName: String,
    val modelName: String,
    val bodyName: String,
    val releaseYear: UShort,
    val gearboxName: String,
    val driveName: String,
    val vehicleWeightKg: UShort,
    val enginePowerHp: UShort,
    val enginePowerKw: Float,
    val engineCapacityL: Float,
    val tankCapacityL: UByte,
    val fuelTypeName: String,
    val personId: UInt,
    val photoId: UInt
)
