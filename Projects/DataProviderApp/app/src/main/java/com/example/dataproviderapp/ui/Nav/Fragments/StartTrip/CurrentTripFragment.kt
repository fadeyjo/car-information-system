package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import com.example.dataproviderapp.R
import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.LineStyle
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.RotationType
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import androidx.core.graphics.createBitmap
import com.yandex.mapkit.map.MapType

class CurrentTripFragment : Fragment() {

    private var ecuId: IntArray? = null
    private var supportedPids: Long? = null

    private var _binding: FragmentCurrentTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    private var obdTask: Job? = null
    private var gpsTask: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var userPlacemark: PlacemarkMapObject? = null
    private var routePolyline: PolylineMapObject? = null
    private val routePoints = mutableListOf<Point>()
    private var startPlacemark: PlacemarkMapObject? = null

    private enum class CameraMode { FOLLOW, FREE }

    private var cameraMode: CameraMode = CameraMode.FOLLOW
    private var gestureInProgress: Boolean = false
    private var lastBearing: Float? = null

    private val positionIconProvider: ImageProvider by lazy(LazyThreadSafetyMode.NONE) {
        imageProviderFromVector(R.drawable.ic_position)
    }

    private val startIconProvider: ImageProvider by lazy(LazyThreadSafetyMode.NONE) {
        imageProviderFromVector(R.drawable.ic_start_point)
    }

    private val gson by lazy { Gson() }

    @Volatile
    private var lastLocation: Location? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCurrentTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
    }

    @SuppressLint("ClickableViewAccessibility")
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

        binding.mapView.mapWindow.map.addCameraListener { _, cameraPosition, reason, finished ->
            if (reason != com.yandex.mapkit.map.CameraUpdateReason.APPLICATION) {
                cameraMode = CameraMode.FREE
                gestureInProgress = !finished
            } else if (finished) {
                gestureInProgress = false
            }

            updatePlacemarkStyleForCamera()
        }

        binding.mapView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    cameraMode = CameraMode.FREE
                    gestureInProgress = true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP,
                MotionEvent.ACTION_CANCEL -> {
                    gestureInProgress = false
                }
            }
            false
        }

        binding.btnFollow.setOnClickListener {
            cameraMode = CameraMode.FOLLOW
            lastLocation?.let { loc ->
                updateMap(loc.latitude, loc.longitude, forceMoveCamera = true)
            }
        }

        binding.mapView.mapWindow.map.mapType = MapType.MAP

        obdClient.startSession(speed) {
            connectionTimeoutToStartSession()
        }
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

    // Управляет доступностью бокового меню
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

    // Остановка сессии
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun endTrip() {
        stopTasks()

        viewModel.obdBleClient?.stopSession {
            connectionTimeoutToStopSession()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                if (location.accuracy > 15) {
                    return
                }

                lastLocation = location

                updateMap(location.latitude, location.longitude)
                updateDirection(if (location.hasBearing()) location.bearing else null)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            requireActivity().mainLooper
        )
    }

    // Расстояние между двумя точками
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

    private fun updateMap(lat: Double, lon: Double, forceMoveCamera: Boolean = false) {
        val point = Point(lat, lon)

        val map = binding.mapView.mapWindow.map
        val mapObjects = map.mapObjects

        if (startPlacemark == null) {
            startPlacemark = mapObjects.addPlacemark().apply {
                geometry = point
                setIcon(startIconProvider)
                setIconStyle(
                    IconStyle().apply {
                        anchor = PointF(0.5f, 0.5f)
                    }
                )
            }
        }

        if (userPlacemark == null) {
            userPlacemark = mapObjects.addPlacemark().apply {
                geometry = Point(point.latitude, point.longitude)
                setIcon(positionIconProvider)
                setIconStyle(
                    IconStyle().apply {
                        anchor = PointF(0.5f, 0.5f)
                        rotationType = RotationType.ROTATE
                    }
                )
            }
        } else {
            userPlacemark?.geometry = point
        }

        updatePlacemarkStyleForCamera()

        val lastPoint = routePoints.lastOrNull()

        var distance = 0.0

        if (lastPoint != null) {
            distance = distanceMeters(
            point.latitude, point.longitude,
            lastPoint.latitude, lastPoint.longitude
            )
        }

        if (lastPoint == null
            || distance > 2
        ) {
            routePoints.add(point)

            if (routePolyline == null) {
                routePolyline = mapObjects.addPolyline(Polyline(routePoints))
                val lineStyle = LineStyle().apply {
                    strokeWidth = 12f
                    outlineWidth = 1f
                    outlineColor = 0xFF000000.toInt()
                }

                routePolyline?.style = lineStyle
                routePolyline?.setStrokeColor(Color.argb(255, 25, 118, 210)) // #1976D2 a = 0
            } else {
                routePolyline?.geometry = Polyline(routePoints)
            }
        }

        val size = routePoints.size
        if (cameraMode == CameraMode.FOLLOW || forceMoveCamera) {
            map.move(
                CameraPosition(point, 17f, 0f, 0f),
                com.yandex.mapkit.Animation(
                    com.yandex.mapkit.Animation.Type.SMOOTH,
                    0.5f
                ),
                null
            )
        }
    }

    private fun updateDirection(bearing: Float?) {
        lastBearing = bearing
        updatePlacemarkDirectionForCamera()
    }

    private fun updatePlacemarkStyleForCamera() {
        userPlacemark?.setIconStyle(
            IconStyle().apply {
                anchor = PointF(0.5f, 0.5f)
                rotationType = RotationType.ROTATE
            }
        )

        updatePlacemarkDirectionForCamera()
    }

    private fun updatePlacemarkDirectionForCamera() {
        val bearing = lastBearing ?: return
        val mapAzimuth = binding.mapView.mapWindow.map.cameraPosition.azimuth
        userPlacemark?.direction = normalizeDegrees(bearing - mapAzimuth)
    }

    private fun normalizeDegrees(value: Float): Float {
        var v = value % 360f
        if (v < 0f) v += 360f
        return v
    }

    private fun imageProviderFromVector(drawableRes: Int): ImageProvider {
        val drawable = AppCompatResources.getDrawable(requireContext(), drawableRes)
            ?: error("Drawable not found: $drawableRes")

        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 64
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 64

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return ImageProvider.fromBitmap(bitmap)
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

                                startLocationUpdates()

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

    @SuppressLint("MissingPermission")
    private suspend fun sendGpsData() {
        val formatter = DateTimeFormatter.ISO_DATE_TIME

        while (currentCoroutineContext().isActive) {

            val location = lastLocation

            if (location == null) {
                delay(200)
                continue
            }

            val data = CreateGpsDataRequest(
                LocalDateTime.now(ZoneOffset.UTC).format(formatter), viewModel.currentTrip!!.tripId,
                location.latitude, location.longitude,
                location.accuracy, (location.speed * 3.6).toInt(),
                location.bearing
            )

            MqttClient.publishJson("gps/new-data", gson.toJson(data))

            delay(500)
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
        startPlacemark = null
        routePolyline = null
        routePoints.clear()

        _binding = null

        locationCallback?.let {
            if (::fusedLocationClient.isInitialized) {
                fusedLocationClient.removeLocationUpdates(it)
            }
        }
        locationCallback = null

        super.onDestroyView()
    }
}