package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import DevicesAdapter
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dataproviderapp.R
import com.example.dataproviderapp.ble.ObdBleClient
import com.example.dataproviderapp.databinding.FragmentSelectDeviceBinding
import com.example.dataproviderapp.ui.Nav.NavViewModel
import java.util.Locale
import kotlin.getValue

class SelectDeviceFragment : Fragment() {
    private var _binding: FragmentSelectDeviceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()
    private var isScanning = false

    private var connectTimeoutHandler: Handler? = null
    private var connectTimeoutRunnable: Runnable? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val allGranted = permissions.all { it.value }

            if (allGranted) {
                bleInitAndScan()
            } else {
                goBackWithMessage("Все разрешения обязательны для работы с bluetooth")
            }
        }


    private lateinit var adapter: DevicesAdapter
    private var scanCallback: ScanCallback? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null


    private fun connected() {
        val speed = binding.actCanSpeed.text.toString().toUShort()

        val fragment = CurrentTripFragment().apply {
            arguments = Bundle().apply {
                putInt("speed", speed.toInt())
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun onDeviceClicked(device: BtDevice) {
        stopScanning()

        viewModel.obdBleClient = ObdBleClient(
            device.device,
            requireContext().applicationContext
        ) {
            requireActivity().runOnUiThread {
                cancelConnectTimeout()
                connected()
            }
        }

        viewModel.obdBleClient!!.connect()
        startConnectTimeout()
    }

    @SuppressLint("MissingPermission")
    private fun startConnectTimeout() {
        connectTimeoutHandler = Handler(Looper.getMainLooper())
        connectTimeoutRunnable = Runnable {
            requireActivity().runOnUiThread {
                AlertDialog.Builder(requireContext())
                    .setTitle("Ошибка")
                    .setMessage("Не удалось подключиться к устройству за 15 секунд")
                    .setPositiveButton("ОК") { dialog, _ ->
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .show()

                adapter.clearDevices()

                if (!isScanning) {
                    startBleScan()
                }
            }
        }
        connectTimeoutHandler?.postDelayed(connectTimeoutRunnable!!, 15000)
    }

    private fun cancelConnectTimeout() {
        connectTimeoutHandler?.removeCallbacks(connectTimeoutRunnable!!)
        connectTimeoutHandler = null
        connectTimeoutRunnable = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelectDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpeed()

        adapter = DevicesAdapter { btDevice ->
            onDeviceClicked(btDevice)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        if (!hasPermissions()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )

            return
        }

        bleInitAndScan()
    }

    private fun setupSpeed() {
        val items = listOf(125, 250, 500)

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        )
        binding.actCanSpeed.setAdapter(adapter)

        val position = adapter.getPosition(500)
        if (position >= 0) {
            binding.actCanSpeed.setText(adapter.getItem(position).toString(), false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun bleInitAndScan() {
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            goBackWithMessage("Ваше устройство не поддерживает технологию bluetooth")
            return
        }

        if (bluetoothAdapter!!.isEnabled) {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

            if (bluetoothLeScanner == null) {
                goBackWithMessage("Ваше устройство не поддерживает технологию ble")

                return
            }

            startBleScan()
        } else {
            goBackWithMessage("Включите bluetooth и попробуйте снова")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startBleScan() {
        val scanner = bluetoothLeScanner ?: return

        scanCallback = object : ScanCallback() {
            @SuppressLint("DefaultLocale")
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val foundDevice = result.device

                if (foundDevice == null) {
                    return
                }

                val name = result.scanRecord?.deviceName ?: foundDevice.name ?: "Unknown"

                if (!name.lowercase(Locale.ROOT).contains("obdii")) {
                    return
                }

                val device = BtDevice(name, foundDevice.address, foundDevice)

                requireActivity().runOnUiThread {
                    adapter.addDevice(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                goBackWithMessage("Во время сканирования произошла ошибка. Попробуйте ещё раз.")
            }
        }

        scanner.startScan(scanCallback)
        isScanning = true
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun goBackWithMessage(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ ->
                requireActivity().supportFragmentManager.popBackStack()
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        if (hasPermissions() && isScanning) {
            scanCallback?.let {
                bluetoothLeScanner?.stopScan(it)
            }
            isScanning = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        stopScanning()

        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        super.onDestroy()

        if (viewModel.obdBleClient != null) {
            viewModel.obdBleClient!!.disconnect()
            viewModel.obdBleClient = null
        }
    }
}