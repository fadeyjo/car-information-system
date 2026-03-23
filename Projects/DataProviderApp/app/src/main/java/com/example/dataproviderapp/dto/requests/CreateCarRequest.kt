package com.example.dataproviderapp.dto.requests

data class CreateCarRequest(
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
    val fuelTypeName: String
)
