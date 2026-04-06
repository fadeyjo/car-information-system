package com.example.dataproviderapp.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import com.example.dataproviderapp.dto.responses.CarDto
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ObdBleClient(
    val device: BluetoothDevice,
    private val context: Context,
    private val connectedCallBack: () -> Unit
) {

    private var connectTimeoutHandler: Handler? = null
    private var connectTimeoutRunnable: Runnable? = null

    private val SERVICE_UUID = UUID.fromString("3a94650f-9ca2-4a5b-a23c-4bf200007d1a")
    private val CMD_UUID     = UUID.fromString("3a94650f-9ca2-4a5b-a23c-4bf200017d1a")
    private val NOTIFY_UUID  = UUID.fromString("3a94650f-9ca2-4a5b-a23c-4bf200027d1a")
    private val NOTIFICATIONS_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var gatt: BluetoothGatt? = null
    private var cmdChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null

    var handleObdData: ((DataCallBack) -> Unit)? = null

    var timeoutFun: (() -> Unit)? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(timeoutCallback: () -> Unit) {
        timeoutFun = timeoutCallback
        startConnectTimeout()
        gatt = device.connectGatt(context, false, callback)
    }

    @SuppressLint("MissingPermission")
    private fun startConnectTimeout() {
        if (timeoutFun == null) {
            return
        }

        connectTimeoutHandler = Handler(Looper.getMainLooper())
        connectTimeoutRunnable = Runnable {
            timeoutFun!!()
        }
        connectTimeoutHandler?.postDelayed(connectTimeoutRunnable!!, 10000)
    }

    private fun cancelConnectTimeout() {
        connectTimeoutHandler?.removeCallbacks(connectTimeoutRunnable!!)
        connectTimeoutHandler = null
        connectTimeoutRunnable = null
        timeoutFun = null
    }

    private val callback = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.close()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.requestMtu(100)
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)

            cancelConnectTimeout()
            gatt?.discoverServices()
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
            val service = gatt.getService(SERVICE_UUID) ?: return

            cmdChar = service.getCharacteristic(CMD_UUID)
            notifyChar = service.getCharacteristic(NOTIFY_UUID)

            if (cmdChar == null || notifyChar == null) {
                return
            }

            enableNotifications(gatt, notifyChar!!)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)

            if (descriptor!!.uuid == NOTIFICATIONS_DESCRIPTOR_UUID) {
                connectedCallBack()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val data = value
            handleNotify(data)
        }

        @Deprecated("Android < 13 callback")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleNotify(characteristic.value ?: return)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(
            NOTIFICATIONS_DESCRIPTOR_UUID
        ) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun readLe16(byteArray: List<Int>, index: Int): Int {
        val value: Int = (byteArray[index + 1] and 0xFF) shl 8

        return value + (byteArray[index] and 0xFF);
    }

    private fun readLe32(byteArray: List<Int>, index: Int): Long {
        return (byteArray[index].toLong() and 0xFF) or
                ((byteArray[index + 1].toLong() and 0xFF) shl 8) or
                ((byteArray[index + 2].toLong() and 0xFF) shl 16) or
                ((byteArray[index + 3].toLong() and 0xFF) shl 24)
    }

    private fun handleNotify(data: ByteArray) {
        val uData = data.map { it.toInt() and 0xFF }

        when (uData[0]) {

            0x01 -> { // SESSION_STARTED
                cancelConnectTimeout()

                val speed = readLe16(uData, 1)
                val pids = readLe32(uData, 3)

                val dataCallback = DataCallBack.SupportedPids(pids)
                handleObdData?.invoke(dataCallback)
            }

            0x02 -> { // OBD_RESPONSE
                val id = readLe32(uData, 1)
                val dlc = uData[5].toShort()
                val payload = uData.slice(6..(5 + dlc.toInt()))

                val dataCallback = DataCallBack.ObdResponse(id, dlc, payload)
                handleObdData?.invoke(dataCallback)
            }

            0x03 -> {
                cancelConnectTimeout()

                val dataCallback = DataCallBack.SessionStopped
                handleObdData?.invoke(dataCallback)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun startSession(speed: UShort, timeoutCallback: () -> Unit) {
        timeoutFun = timeoutCallback

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
    fun stopSession(timeoutCallback: () -> Unit) {
        timeoutFun = timeoutCallback
        write(byteArrayOf(0x03))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun write(data: ByteArray) {
        if (cmdChar == null || gatt == null) {
            return
        }

        startConnectTimeout()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt!!.writeCharacteristic(
                cmdChar!!,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            @Suppress("DEPRECATION")
            run {
                cmdChar!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                cmdChar!!.value = data
                gatt!!.writeCharacteristic(cmdChar)
            }
        }
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
            val pids: Long
        ): DataCallBack()
        data class ObdResponse(
            val id: Long,
            val dlc: Short,
            val data: List<Int>
        ) : DataCallBack()
        object SessionStopped: DataCallBack()
    }
}