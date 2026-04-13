package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import com.example.dataproviderapp.R
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dataproviderapp.ble.ObdBleClient
import com.example.dataproviderapp.databinding.FragmentCurrentTripBinding
import com.example.dataproviderapp.dto.requests.CreateGpsDataRequest
import com.example.dataproviderapp.dto.requests.CreateTelemetryDataRequest
import com.example.dataproviderapp.mqtt.MqttClient
import com.example.dataproviderapp.ui.Nav.CurrentDataSupportedPidsDetailsState
import com.example.dataproviderapp.ui.Nav.NavActivity
import com.example.dataproviderapp.ui.Nav.NavViewModel
import com.example.dataproviderapp.ui.Nav.StartTripState
import com.example.dataproviderapp.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.getValue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CurrentTripFragment : Fragment() {

    private var ecuId: IntArray? = null
    private var supportedPids: Long? = null

    private var _binding: FragmentCurrentTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    private var obdTask: Job? = null
    private var gpsTask: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var userPlacemark: PlacemarkMapObject? = null
    private var routePolyline: PolylineMapObject? = null
    private val routePoints = mutableListOf<Point>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCurrentTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey("3c93913d-2dfe-4a57-af26-6b270706fb3a")
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

        observeVIewModel()

        binding.mapView.mapWindow.map.move(
            CameraPosition(
                Point(59.0, 30.0),
                15.0f,
                0.0f,
                0.0f
            )
        )

        viewModel.obdBleClient!!.startSession(speed) {
            connectionTimeoutToStartSession()
        }
    }

    private fun updateMap(lat: Double, lon: Double) {
        val point = Point(lat, lon)

        val mapObjects = binding.mapView.mapWindow.map.mapObjects

        if (userPlacemark == null) {
            userPlacemark = mapObjects.addPlacemark().apply {
                geometry = point
                setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_arrow))
            }
        } else {
            userPlacemark?.geometry = point
        }

        routePoints.add(point)

        if (routePolyline == null) {
            routePolyline = mapObjects.addPolyline(Polyline(routePoints))
        } else {
            routePolyline?.geometry = Polyline(routePoints)
        }

        binding.mapView.mapWindow.map.move(
            CameraPosition(point, 16f, 0f, 0f)
        )
    }

    private fun updateDirection(bearing: Float?) {
        if (bearing == null) {
            return
        }

        userPlacemark?.direction = bearing
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
    private fun intToBase64(value: Long): String {
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
                val bytes = data.data.map { it.toByte() }.toByteArray()

                val formatter = DateTimeFormatter.ISO_DATE_TIME

                val content = CreateTelemetryDataRequest(
                    LocalDateTime.now(ZoneOffset.UTC).format(formatter), 1.toUByte(), data.data[2].toUShort(),
                    intToBase64(data.id), data.dlc.toUByte(), Base64.encode(bytes),
                    viewModel.currentTrip!!.tripId
                )

                val gson = Gson()

                val json = gson.toJson(content)

                MqttClient.publishJson("telemetry/new-data", json)
            }
            ObdBleClient.DataCallBack.SessionStopped -> TODO()
            is ObdBleClient.DataCallBack.SupportedPids -> {
                ecuId = data.ecuId
                supportedPids = data.pids
                viewModel.getCurrentDataSupportedPids(data.pids)
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
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
                                MqttClient.connectMqtt()
                                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

                                viewModel.currentTrip = state.trip

                                stopTasks()

                                obdTask = launch {
                                    sendObdData()
                                }

                                gpsTask = launch {
                                    sendGpsData()
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
                                val base64Ecu = ByteArray(ecuId!!.size) { i -> ecuId!![i].toByte() }

                                viewModel.startTrip(
                                    LocalDateTime.now(ZoneOffset.UTC),
                                    viewModel.obdBleClient!!.device.address,
                                    viewModel.selectedCarToTrip!!.carId,
                                    Base64.encode(base64Ecu), supportedPids!!
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
        if (viewModel.curDataPids == null || viewModel.obdBleClient == null) {
            return
        }

        val once = viewModel.curDataPids!!.once
        val repeatable = viewModel.curDataPids!!.repeatable

        for (i in 0..(once.size - 1)) {
            viewModel.obdBleClient!!.getData(1, once[i])
            delay(200)
        }

        var i = 0

        if (repeatable.isEmpty()) {
            return
        }

        while (currentCoroutineContext().isActive) {
            viewModel.obdBleClient!!.getData(1, repeatable[i++])

            if (repeatable.size == i) {
                i = 0
            }

            delay(200)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getCurrentGpsData(): CreateGpsDataRequest? {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val location = fusedLocationClient.lastLocation.await()

        if (location != null) {

            val lat = location.latitude
            val lon = location.longitude
            val accuracy = if (location.hasAccuracy()) location.accuracy else null
            val speed = if (location.hasSpeed()) (location.speed * 3.6).toInt() else null
            val bearing = if (location.hasBearing()) location.bearing else null

            requireActivity().runOnUiThread {
                updateMap(lat, lon)
            }

            return CreateGpsDataRequest(
                LocalDateTime.now(ZoneOffset.UTC).format(formatter), viewModel.currentTrip!!.tripId,
                lat, lon,
                accuracy, speed,
                bearing
            )
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendGpsData() {
        val gson = Gson()

        while (currentCoroutineContext().isActive) {

            val data = getCurrentGpsData()

            if (data == null) {
                continue
            }

            requireActivity().runOnUiThread {
                updateMap(data.latitudeDeg, data.longitudeDeg)
                updateDirection(data.bearingDeg)
            }

            val json = gson.toJson(data)

            MqttClient.publishJson("gps/new-data", json)

            delay(1000)
        }
    }

    private fun stopTasks() {
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

        MqttClient.disconnect()

        viewModel.currentTrip = null
        viewModel.curDataPids = null
    }
}