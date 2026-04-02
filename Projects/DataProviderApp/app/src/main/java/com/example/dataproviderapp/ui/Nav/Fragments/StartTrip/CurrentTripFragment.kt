package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import android.Manifest
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
import com.example.dataproviderapp.R
import com.example.dataproviderapp.ble.ObdBleClient
import com.example.dataproviderapp.databinding.FragmentCurrentTripBinding
import com.example.dataproviderapp.databinding.FragmentSelectDeviceBinding
import com.example.dataproviderapp.ui.Nav.NavActivity
import com.example.dataproviderapp.ui.Nav.NavViewModel
import kotlin.getValue

class CurrentTripFragment : Fragment() {

    private var _binding: FragmentCurrentTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null || arguments?.getInt("speed") == null) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        changeStateNav(true)

        val myUShort: UShort = arguments?.getInt("speed")?.toUShort() ?: 0u

        viewModel.obdBleClient!!.handleObdData = { data ->
            dataCallback(data)
        }

        viewModel.obdBleClient!!.startSession(myUShort)
    }

    private fun dataCallback(data: ObdBleClient.DataCallBack) {
        when (data) {
            is ObdBleClient.DataCallBack.Error -> showErrorDialog(data.message)
            is ObdBleClient.DataCallBack.ObdResponse -> TODO()
            ObdBleClient.DataCallBack.SessionStopped -> TODO()
            is ObdBleClient.DataCallBack.SupportedPids -> TODO()
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroyView() {
        super.onDestroyView()

        changeStateNav(false)

        if (viewModel.obdBleClient != null) {
            viewModel.obdBleClient!!.disconnect()
        }
        viewModel.obdBleClient = null
    }
}