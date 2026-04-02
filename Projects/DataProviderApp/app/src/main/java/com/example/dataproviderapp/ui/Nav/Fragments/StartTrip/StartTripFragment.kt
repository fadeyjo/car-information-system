package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.FragmentProfileBinding
import com.example.dataproviderapp.databinding.FragmentStartTripBinding
import com.example.dataproviderapp.ui.Nav.NavViewModel
import kotlin.getValue

class StartTripFragment : Fragment() {

    private var _binding: FragmentStartTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStartTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel();

        binding.btnStartTrip.setOnClickListener {
            startTrip()
        }
    }

    private fun observeViewModel() {

    }

    private fun startTrip() {
        // Здесь необходимо начать поездку:
        // 1) Открыть фрагмент, в котором необходимо выбрать BLE устройство для подключения:
        // этот фрагмент представляет из себя просто список из устройств (имя + MAC address),
        // при клике на элемент списка происходит подключение к выбранному устройству,
        // так же на этом фрагменте через выпадающий список предусмотреть выбор скорости CAN (125, 250 или 500)
        // 2) После успешного подключения на устройство посылается команда о начале сессии вместе с выбранной скорость CAN
    }
}