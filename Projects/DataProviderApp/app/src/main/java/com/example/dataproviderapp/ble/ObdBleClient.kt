package com.example.dataproviderapp.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.dataproviderapp.dto.responses.CarDto
import java.util.UUID

class ObdBleClient(
    private val device: BluetoothDevice,
    private val context: Context,
    private val connectedCallBack: () -> Unit
) {

    private val SERVICE_UUID = UUID.fromString("3a94650f-9ca2-4a5b-a23c-4bf200007d1a")
    private val CMD_UUID     = UUID.fromString("3a94650f-9ca2-4a5b-a23c-4bf200017d1a")
    private val NOTIFY_UUID  = UUID.fromString("3a94650f-9ca2-4a5b-a23c-4bf200027d1a")
    private val NOTIFICATIONS_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var gatt: BluetoothGatt? = null
    private var cmdChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null

    var handleObdData: ((DataCallBack) -> Unit)? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect() {
        gatt = device.connectGatt(context, false, callback)
    }

    private val callback = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                println("Error in onCharacteristicWrite")
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID)

            cmdChar = service.getCharacteristic(CMD_UUID)
            notifyChar = service.getCharacteristic(NOTIFY_UUID)

            if (cmdChar == null || notifyChar == null) {
                return
            }

            enableNotifications(gatt, notifyChar!!)

            connectedCallBack()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val data = value
            handleNotify(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(
            NOTIFICATIONS_DESCRIPTOR_UUID
        ) ?: return

        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
    }

    private fun readLe16(byteArray: ByteArray, index: Int): UShort {
        val value: UShort = (byteArray[index + 1].toUInt() shl 8).toUShort()

        return (value + byteArray[index].toUShort()).toUShort();
    }

    private fun readLe32(byteArray: ByteArray, index: Int): UInt {
        return byteArray[index].toUInt() +
                (byteArray[index + 1].toUInt() shl 8) +
                (byteArray[index + 2].toUInt() shl 16) +
                (byteArray[index + 3].toUInt() shl 24);
    }

    private fun handleNotify(data: ByteArray) {
        when (data[0].toInt() and 0xFF) {

            0x01 -> { // SESSION_STARTED
                val speed = readLe16(data, 1)
                val pids = readLe32(data, 3)

                println("Session started: speed=$speed pids=$pids")
            }

            0x02 -> { // OBD_RESPONSE
                val id = readLe32(data, 1)
                val dlc = data[5].toUByte()
                val payload = data.copyOfRange(6, 6 + dlc.toInt())

                println("OBD: id=$id data=${payload.joinToString()}")
            }

            0x03 -> {
                println("Session stopped")
            }

            0xFF -> {
                val len = data[1].toUByte()
                val msg = String(data, 2, len.toInt())
                println("Error: $msg")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun startSession(speed: UShort) {
        val data = ByteArray(3)
        data[0] = 0x01

        data[1] = (speed.toInt() and 0xFF).toByte()
        data[2] = ((speed.toInt() shr 8) and 0xFF).toByte()

        write(data)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getData(mode: Int, pid: Int) {
        val data = byteArrayOf(
            0x02,
            mode.toByte(),
            pid.toByte()
        )
        write(data)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun stopSession() {
        write(byteArrayOf(0x03))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun write(data: ByteArray) {
        if (cmdChar == null || gatt == null) {
            return
        }

        gatt!!.writeCharacteristic(
            cmdChar!!,
            data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        gatt?.let {
            notifyChar?.let { char ->
                val descriptor = char.getDescriptor(NOTIFICATIONS_DESCRIPTOR_UUID)
                descriptor?.let { d ->
                    gatt?.writeDescriptor(d, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
                }
            }

            gatt?.disconnect()
            gatt?.close()
        }

        gatt = null
        cmdChar = null
        notifyChar = null
    }

    sealed class DataCallBack{
        data class SupportedPids(
            val pids: UInt
        ): DataCallBack()
        data class ObdResponse(
            val id: UInt,
            val dlc: UByte,
            val data: ByteArray
        ) : DataCallBack() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ObdResponse

                if (id != other.id) return false
                if (dlc != other.dlc) return false
                if (!data.contentEquals(other.data)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = id.hashCode()
                result = 31 * result + dlc.hashCode()
                result = 31 * result + data.contentHashCode()
                return result
            }
        }
        object SessionStopped: DataCallBack()
        data class Error(
            val message: String
        ): DataCallBack()
    }
}