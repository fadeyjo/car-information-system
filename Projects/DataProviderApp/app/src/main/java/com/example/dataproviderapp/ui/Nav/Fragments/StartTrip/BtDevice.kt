package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import android.bluetooth.BluetoothDevice

data class BtDevice(
    val deviceName: String,
    val macAddress: String,
    val device: BluetoothDevice
)
