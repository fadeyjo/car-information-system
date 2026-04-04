package com.example.dataproviderapp.ui.Nav.Fragments.MyCars

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.FragmentCarDetailsBinding
import com.example.dataproviderapp.dto.responses.CarDto
import com.example.dataproviderapp.ui.Nav.Fragments.Profile.UpdatePersonFragment
import com.example.dataproviderapp.ui.Nav.NavViewModel
import com.example.dataproviderapp.utils.Utils
import kotlin.getValue

class CarDetailsFragment : Fragment() {

    private var _binding: FragmentCarDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.selectedCarToDetail == null) {
            Utils.showErrorDialogWithAction("Не удалось найти автомобиль", requireContext()) {
                parentFragmentManager.popBackStack()
            }

            return
        }

        val car = viewModel.selectedCarToDetail!!

        binding.btnEdit.setOnClickListener {
            redactCar()
        }

        renderCar(car)
    }

    private fun redactCar() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, UpdateCarFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun renderCar(car: CarDto) {
        Glide.with(this)
            .load("${BuildConfig.BASE_URL}car-photos/${car.photoId}")
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .into(binding.ivCarPhoto)

        binding.tvTitle.text = "${car.brandName} ${car.modelName}"
        binding.tvVin.text = "VIN: ${car.vinNumber}"
        binding.tvStateNumber.text = if (car.stateNumber.isNullOrBlank()) {
            "Гос. номер: -"
        } else {
            "Гос. номер: ${car.stateNumber}"
        }
        binding.tvBody.text = "Тип кузова: ${car.bodyName}"
        binding.tvReleaseYear.text = "Год выпуска: ${car.releaseYear}"
        binding.tvGearbox.text = "Коробка передач: ${car.gearboxName}"
        binding.tvDrive.text = "Привод: ${car.driveName}"
        binding.tvWeight.text = "Масса: ${car.vehicleWeightKg} кг"
        binding.tvEnginePowerHp.text = "Мощность двигателя: ${car.enginePowerHp} л.с."
        binding.tvEnginePowerKw.text = "Мощность двигателя (кВт): ${car.enginePowerKw} кВт"
        binding.tvEngineCapacity.text = "Объем двигателя: ${car.engineCapacityL} л"
        binding.tvTankCapacity.text = "Объем бака: ${car.tankCapacityL} л"
        binding.tvFuelType.text = "Тип топлива: ${car.fuelTypeName}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
