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
import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.ble.ObdBleClient
import com.example.dataproviderapp.databinding.FragmentCurrentTripBinding
import com.example.dataproviderapp.dto.requests.CreateGpsDataRequest
import com.example.dataproviderapp.dto.requests.CreateTelemetryDataRequest
import com.example.dataproviderapp.mqtt.MqttClient
import com.example.dataproviderapp.ui.Nav.CurrentDataSupportedPidsDetailsState
import com.example.dataproviderapp.ui.Nav.EndTripState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

    private val gson by lazy { Gson() }


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

        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
    }

    private fun changeStateNav(locked: Boolean) {
        val navActivity = activity as? NavActivity

        navActivity?.binding?.drawerLayout?.setDrawerLockMode(
            if (locked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
        )
    }

    private inline fun withBinding(block: FragmentCurrentTripBinding.() -> Unit) {
        _binding?.block()
    }

    private inline fun postToUi(crossinline block: FragmentCurrentTripBinding.() -> Unit) {
        _binding?.root?.post { withBinding(block) }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val speedArg = arguments?.getInt("speed")
        if (speedArg == null) {
            Utils.showErrorDialogWithAction("Не получена скорость", requireContext()) {
                parentFragmentManager.popBackStack()
            }
            return
        }

        changeStateNav(true)

        val speed: UShort = speedArg.toUShort()

        val obdClient = requireNotNull(viewModel.obdBleClient)
        obdClient.handleObdData = ::dataCallback

        observeViewModel()

        binding.mapView.mapWindow.map.move(
            CameraPosition(
                Point(59.0, 30.0),
                15.0f,
                0.0f,
                0.0f
            )
        )

        binding.btnEndTrip.setOnClickListener {
            endTrip()
        }

        obdClient.startSession(speed) {
            connectionTimeoutToStartSession()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun endTrip() {
        stopTasks()

        viewModel.obdBleClient?.stopSession {
            connectionTimeoutToStopSession()
        }
    }

    private fun distanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {

        val R = 6371000.0 // радиус Земли в метрах

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return abs(R * c)
    }

    private fun updateMap(lat: Double, lon: Double) {
        val point = Point(lat, lon)

        val mapObjects = _binding?.mapView?.mapWindow?.map?.mapObjects ?: return

        if (userPlacemark == null) {
            userPlacemark = mapObjects.addPlacemark().apply {
                geometry = point
                setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_arrow))
            }
        } else {
            userPlacemark?.geometry = point
        }


        val lastPoint = routePoints.lastOrNull()


        if (lastPoint != null) {
            val distance = distanceMeters(point.latitude, point.longitude, lastPoint.latitude, lastPoint.longitude)

            if (distance > 3) {
                routePoints.add(point)
            }
        } else {
            routePoints.add(point)
        }

        if (routePolyline == null) {
            routePolyline = mapObjects.addPolyline(Polyline(routePoints))
        } else {
            routePolyline?.geometry = Polyline(routePoints)
        }

        withBinding {
            mapView.mapWindow.map.move(
                CameraPosition(point, 16f, 0f, 0f)
            )
        }
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

    private fun connectionTimeoutToStopSession() {
        goBackWithMessage("Не удалось завершить сессию.")
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

                val json = gson.toJson(content)

                MqttClient.publishJson("telemetry/new-data", json)
            }
            ObdBleClient.DataCallBack.SessionStopped -> {
                viewModel.endTrip(LocalDateTime.now(ZoneOffset.UTC), viewModel.currentTrip!!.tripId)
            }
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
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.startTripState.collect { state ->
                        when (state) {
                            StartTripState.CarNotFound -> goBackWithMessage("Автомобиль не найден")
                            is StartTripState.Data -> {
                                MqttClient.connectMqtt()
                                fusedLocationClient =
                                    LocationServices.getFusedLocationProviderClient(requireContext())

                                viewModel.currentTrip = state.trip

                                MqttClient.subscribeTripTelemetry(
                                    tripId = state.trip.tripId,
                                    onSpeed = { speed ->
                                        postToUi { tvSpeed.text = speed.toString() }
                                    },
                                    onRpm = { rpm ->
                                        postToUi { tvRpm.text = rpm.toString() }
                                    },
                                    onTemp = { temp ->
                                        postToUi { tvTemp.text = temp.toString() }
                                    },
                                )

                                stopTasks()
                                obdTask = launch { sendObdData() }
                                gpsTask = launch { sendGpsData() }
                            }

                            StartTripState.NetworkError -> goBackWithMessage("Нет подключения к интернету")
                            StartTripState.UnknownError -> goBackWithMessage("Неизвестная ошибка")
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.endTripState.collect { state ->
                        when (state) {
                            EndTripState.Ended -> parentFragmentManager.popBackStack()
                            EndTripState.InvalidDatetime -> Utils.showErrorDialogWithAction(
                                "Время окончания поездки должно быть позже времени начала",
                                requireContext()
                            ) {}

                            EndTripState.NetworkError -> {
                                Utils.showNetworkErrorDialog(requireContext())
                                parentFragmentManager.popBackStack()
                            }

                            EndTripState.TripAlreadyEnded -> Utils.showErrorDialogWithAction(
                                "Поездка уже закончена",
                                requireContext()
                            ) {
                                parentFragmentManager.popBackStack()
                            }

                            EndTripState.TripNotFound -> Utils.showErrorDialogWithAction(
                                "Поездка не найдена",
                                requireContext()
                            ) {
                                parentFragmentManager.popBackStack()
                            }

                            EndTripState.UnknownError -> {
                                Utils.showUnknownErrorDialog(requireContext())
                                parentFragmentManager.popBackStack()
                            }

                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.currentDataSupportedPidsState.collect { state ->
                        when (state) {
                            CurrentDataSupportedPidsDetailsState.NetworkError -> goBackWithMessage("Нет подключения к интернету")
                            is CurrentDataSupportedPidsDetailsState.PidsDetails -> {
                                val ecu = ecuId
                                val supported = supportedPids
                                val obdClient = viewModel.obdBleClient
                                val selectedCar = viewModel.selectedCarToTrip

                                if (ecu == null || supported == null || obdClient == null || selectedCar == null) {
                                    return@collect
                                }

                                val base64Ecu = ByteArray(ecu.size) { i -> ecu[i].toByte() }
                                viewModel.startTrip(
                                    LocalDateTime.now(ZoneOffset.UTC),
                                    obdClient.device.address,
                                    selectedCar.carId,
                                    Base64.encode(base64Ecu),
                                    supported
                                )
                            }

                            CurrentDataSupportedPidsDetailsState.UnknownError -> goBackWithMessage("Неизвестная ошибка")
                            else -> Unit
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
            delay(100)
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

            delay(100)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getCurrentGpsData(): CreateGpsDataRequest? {
        if (!::fusedLocationClient.isInitialized) {
            return null
        }

        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val location = fusedLocationClient.lastLocation.await()

        if (location != null) {

            val lat = location.latitude
            val lon = location.longitude
            val accuracy = if (location.hasAccuracy()) location.accuracy else null
            val speed = if (location.hasSpeed()) (location.speed * 3.6).toInt() else null
            val bearing = if (location.hasBearing()) location.bearing else null

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
        while (currentCoroutineContext().isActive) {

            val data = getCurrentGpsData()

            if (data == null) {
                delay(50)
                continue
            }

            withContext(Dispatchers.Main) {
                updateMap(data.latitudeDeg, data.longitudeDeg)
                updateDirection(data.bearingDeg)
            }

            val json = gson.toJson(data)

            MqttClient.publishJson("gps/new-data", json)

            delay(100)
        }
    }

    private fun stopTasks() {
        obdTask?.cancel()
        gpsTask?.cancel()
        obdTask = null
        gpsTask = null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroyView() {
        stopTasks()

        changeStateNav(false)

        viewModel.obdBleClient?.handleObdData = null
        viewModel.obdBleClient?.disconnect()
        viewModel.obdBleClient = null

        viewModel.currentTrip?.let { MqttClient.unsubscribeTripTelemetry(it.tripId) }

        MqttClient.disconnect()

        viewModel.currentTrip = null
        viewModel.curDataPids = null

        userPlacemark = null
        routePolyline = null
        routePoints.clear()

        _binding = null
        super.onDestroyView()
    }
}