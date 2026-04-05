package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dataproviderapp.R
import com.example.dataproviderapp.ble.ObdBleClient
import com.example.dataproviderapp.databinding.FragmentCurrentTripBinding
import com.example.dataproviderapp.databinding.FragmentSelectDeviceBinding
import com.example.dataproviderapp.dto.requests.CreateTelemetryDataRequest
import com.example.dataproviderapp.ui.Nav.CurrentDataSupportedPidsDetailsState
import com.example.dataproviderapp.ui.Nav.NavActivity
import com.example.dataproviderapp.ui.Nav.NavViewModel
import com.example.dataproviderapp.ui.Nav.StartTripState
import com.example.dataproviderapp.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.getValue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CurrentTripFragment : Fragment() {

    private var _binding: FragmentCurrentTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    private var obdTask: Job? = null
    private var gpsTask: Job? = null

    private val brokerUrl = "tcp://185.120.59.21:1883"
    private val clientId = "android-kotlin-client"
    private var client: MqttClient? = null


    fun connectMqtt() {
        client = MqttClient(brokerUrl, clientId, null)

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        client!!.connect(options)
    }

    fun publishJson(topic: String, json: String) {
        if (client == null) {
            return
        }

        val message = MqttMessage(json.toByteArray()).apply {
            qos = 1
            isRetained = false
        }

        client!!.publish(topic, message)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCurrentTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun changeStateNav(locked: Boolean) {
        val navActivity = activity as? NavActivity

        if (locked) {
            navActivity?.binding?.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            navActivity?.binding?.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null || arguments?.getInt("speed") == null) {
            Utils.showErrorDialogWithAction("Не получена скорость", requireContext()) {
                parentFragmentManager.popBackStack()
            }
            return
        }

        changeStateNav(true)

        val speed: UShort = arguments?.getInt("speed")?.toUShort() ?: 0u

        viewModel.obdBleClient!!.handleObdData = { data ->
            dataCallback(data)
        }

        connectMqtt()

        observeVIewModel()

        viewModel.obdBleClient!!.startSession(speed) {
            connectionTimeoutToStartSession()
        }
    }

    private fun connectionTimeoutToStartSession() {
        goBackWithMessage("Не удалось начать сессию, вероятнее всего выбрана некорректная скорость CAN. Повторите попытку с другой скоростью.")
    }

    private fun goBackWithMessage(message: String) {
        Utils.showErrorDialogWithAction(message, requireContext()) {
            parentFragmentManager.popBackStack()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun uintToBase64(value: UInt): String {
        val bytes = byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
        return Base64.encode(bytes)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun dataCallback(data: ObdBleClient.DataCallBack) {
        when (data) {
            is ObdBleClient.DataCallBack.ObdResponse -> {
                val content = CreateTelemetryDataRequest(
                    LocalDateTime.now(ZoneOffset.UTC), 1.toUByte(), data.data[2].toUShort(),
                    uintToBase64(data.id), data.dlc, Base64.encode(data.data),
                    viewModel.currentTrip!!.tripId
                )

                val gson = Gson()

                val json = gson.toJson(content)

                publishJson("telemetry/new-data", json)
            }
            ObdBleClient.DataCallBack.SessionStopped -> TODO()
            is ObdBleClient.DataCallBack.SupportedPids -> {
                viewModel.getCurrentDataSupportedPids(data.pids)
            }

            is ObdBleClient.DataCallBack.Error -> TODO()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private fun observeVIewModel() {
        lifecycleScope.launch {
            launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.startTripState.collect { state ->
                        when (state) {
                            StartTripState.CarNotFound -> goBackWithMessage("Автомобиль не найден")
                            is StartTripState.Data -> {
                                viewModel.currentTrip = state.trip

                                stopTasks()

                                obdTask = launch {
                                    sendObdData()
                                }

                                gpsTask = launch {
                                    // sendGpsData()
                                }
                            }
                            StartTripState.NetworkError -> goBackWithMessage("Нет подключения к интернету")
                            StartTripState.UnknownError -> goBackWithMessage("Неизвестная ошибка")
                            else -> {}
                        }
                    }
                }
            }

            launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.currentDataSupportedPidsState.collect { state ->
                        when (state) {
                            CurrentDataSupportedPidsDetailsState.NetworkError -> goBackWithMessage("Нет подключения к интернету")
                            is CurrentDataSupportedPidsDetailsState.PidsDetails -> {
                                viewModel.startTrip(
                                    LocalDateTime.now(ZoneOffset.UTC),
                                    viewModel.obdBleClient!!.device.address,
                                    viewModel.selectedCarToTrip!!.carId
                                )
                            }
                            CurrentDataSupportedPidsDetailsState.UnknownError -> goBackWithMessage("Неизвестная ошибка")

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun sendObdData() {
        viewModel.obdBleClient!!.getData(1, 12.toUShort())
        return
        //отладка выше удалить
        if (viewModel.curDataPids == null) {
            return
        }

        val once = viewModel.curDataPids!!.once
        val repeatable = viewModel.curDataPids!!.repeatable

        for (i in 0..(once.size - 1)) {
            viewModel.obdBleClient!!.getData(1, once[i])
            delay(200)
        }

        var i = 0

        while (currentCoroutineContext().isActive) {
            viewModel.obdBleClient!!.getData(1, repeatable[i])

            if (repeatable.size - 1 == i) {
                i = 0
            } else {
                i++
            }

            delay(200)
        }
    }

    fun stopTasks() {
        obdTask?.cancel()
        gpsTask?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroyView() {
        super.onDestroyView()

        stopTasks()

        changeStateNav(false)

        viewModel.obdBleClient?.disconnect()
        viewModel.obdBleClient = null
    }
}