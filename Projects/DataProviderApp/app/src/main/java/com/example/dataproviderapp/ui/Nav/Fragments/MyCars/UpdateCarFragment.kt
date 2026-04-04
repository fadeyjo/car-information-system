package com.example.dataproviderapp.ui.Nav.Fragments.MyCars

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.FragmentUpdateCarBinding
import com.example.dataproviderapp.databinding.FragmentUpdatePersonBinding
import com.example.dataproviderapp.dto.responses.CarDto
import com.example.dataproviderapp.jwtutils.TokenStorage
import com.example.dataproviderapp.ui.Nav.CheckBoxesDataState
import com.example.dataproviderapp.ui.Nav.NavViewModel
import com.example.dataproviderapp.ui.Nav.UpdateCarState
import com.example.dataproviderapp.ui.Nav.UpdatePersonState
import com.example.dataproviderapp.ui.SignIn.SignInActivity
import com.example.dataproviderapp.utils.Utils
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.getValue
import kotlin.io.readBytes

class UpdateCarFragment : Fragment() {
    private var _binding: FragmentUpdateCarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedPhotoUri = it
                binding.ivCarPhoto.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUpdateCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivCarPhoto.setOnClickListener {
            pickImageFromGallery()
        }

        binding.btnUpdate.setOnClickListener {
            redactData()
        }

        parentFragmentManager.setFragmentResultListener(
            CreateCarFragment.Companion.REQUEST_SELECT_BRAND,
            viewLifecycleOwner
        ) { _, bundle ->
            val brand = bundle.getString(CreateCarFragment.Companion.KEY_BRAND) ?: return@setFragmentResultListener
            applyBrandSelection(brand)
        }
        parentFragmentManager.setFragmentResultListener(
            CreateCarFragment.Companion.REQUEST_SELECT_MODEL,
            viewLifecycleOwner
        ) { _, bundle ->
            val model = bundle.getString(CreateCarFragment.Companion.KEY_MODEL) ?: return@setFragmentResultListener
            binding.etModel.setText(model)
        }

        binding.etBrand.setOnClickListener {
            selectBrand()
        }

        binding.etModel.setOnClickListener {
            selectModel()
        }

        if (viewModel.selectedCarToDetail == null) {
            Utils.showErrorDialogWithAction("Не удалось найти автомобиль", requireContext()) {
                parentFragmentManager.popBackStack()
            }
            return
        }

        renderCarData(viewModel.selectedCarToDetail!!)

        viewModel.resetUpdateCarState()

        observeVieModel()

        viewModel.getCheckboxesData()
    }

    private fun selectBrand() {
        binding.tilModel.error = null
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SelectBrandFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun selectModel() {
        val brand = binding.etBrand.text?.toString()?.trim().orEmpty()
        if (brand.isEmpty()) {
            binding.tilModel.error = "Сначала выберите марку"
            return
        }
        binding.tilModel.error = null
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SelectModelFragment.newInstance(brand))
            .addToBackStack(null)
            .commit()
    }

    private fun applyBrandSelection(selectedBrand: String) {
        val current = binding.etBrand.text?.toString()?.trim().orEmpty()
        when {
            current.isEmpty() -> binding.etBrand.setText(selectedBrand)
            current.equals(selectedBrand, ignoreCase = true) -> { }
            else -> {
                binding.etBrand.setText(selectedBrand)
                binding.etModel.text?.clear()
            }
        }
    }

    private fun renderCarData(car: CarDto) {
        binding.etVin.setText(car.vinNumber)
        if (!car.stateNumber.isNullOrBlank()) {
            binding.etStateNumber.setText(car.stateNumber)
        }
        binding.etBrand.setText(car.brandName)
        binding.etModel.setText(car.modelName)
        binding.etReleaseYear.setText(car.releaseYear.toString())
        binding.etWeightKg.setText(car.vehicleWeightKg.toString())
        binding.etEnginePowerHp.setText(car.enginePowerHp.toString())
        binding.etEnginePowerKw.setText(car.enginePowerKw.toString())
        binding.etEngineCapacityL.setText(car.engineCapacityL.toString())
        binding.etTankCapacityL.setText(car.tankCapacityL.toString())

        loadCarPhoto(binding.ivCarPhoto, car.photoId)
    }

    private fun observeVieModel() {
        lifecycleScope.launch {
            launch {
                repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    viewModel.checkBoxesDataState.collect { state ->
                        when (state) {
                            is CheckBoxesDataState.Data -> {
                                val car = viewModel.selectedCarToDetail!!

                                setupDropdown(state.bodies, binding.actBody, car.bodyName)
                                setupDropdown(state.gearboxes, binding.actGearbox, car.gearboxName)
                                setupDropdown(state.drives, binding.actDrive, car.driveName)
                                setupDropdown(state.fuelTypes, binding.actFuelType, car.fuelTypeName)


                            }
                            CheckBoxesDataState.SomeError -> Utils.showErrorDialog("Ошибка получения данных", requireContext())

                            else -> {}
                        }
                    }
                }
            }

            launch {
                repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    viewModel.updateCarState.collect { state ->
                        binding.btnUpdate.isEnabled = true

                        when (state) {
                            UpdateCarState.CarExistsByStateNumber -> binding.tilStateNumber.error = "Автомобиль с данным гос номером уже существует"
                            UpdateCarState.CarExistsByVin -> binding.tilVin.error = "Автомобиль с данным VIN уже существует"
                            UpdateCarState.CarNotFound -> {
                                Utils.showErrorDialogWithAction("Автомобиль не найден", requireContext()) {
                                    TokenStorage.clear()
                                    startSignInActivity()
                                }
                            }
                            UpdateCarState.NetworkError -> Utils.showNetworkErrorDialog(requireContext())
                            UpdateCarState.UnknownError -> Utils.showUnknownErrorDialog(requireContext())
                            UpdateCarState.Updated -> parentFragmentManager.popBackStack()
                            is UpdateCarState.ValidationError -> {
                                state.errors.forEach { fieldErrorMap ->
                                    fieldErrorMap.forEach { (field, message) ->
                                        when (field.lowercase()) {
                                            "vinnumber" -> binding.tilVin.error = message
                                            "statenumber" -> binding.tilStateNumber.error = message
                                            "brandname" -> binding.tilBrand.error = message
                                            "modelname" -> binding.tilModel.error = message
                                            "bodyname" -> binding.tilBody.error = message
                                            "releaseyear" -> binding.tilReleaseYear.error = message
                                            "gearboxname" -> binding.tilGearbox.error = message
                                            "drivename" -> binding.tilDrive.error = message
                                            "vehicleweightkg" -> binding.tilWeightKg.error = message
                                            "enginepowerhp" -> binding.tilEnginePowerHp.error = message
                                            "enginepowerkw" -> binding.tilEnginePowerKw.error = message
                                            "enginecapacityl" -> binding.tilEngineCapacityL.error = message
                                            "tankcapacityl" -> binding.tilTankCapacityL.error = message
                                            "fueltypename" -> binding.tilFuelType.error = message
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun setupDropdown(
        items: List<String>,
        view: AutoCompleteTextView,
        text: String
    ) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        )
        view.setAdapter(adapter)

        val position = adapter.getPosition(text)
        if (position >= 0) {
            view.setText(adapter.getItem(position), false)
        }
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun redactData() {
        var isValid = true

        val brand = binding.etBrand.text.toString()
        val model = binding.etModel.text.toString()
        val body = binding.actBody.text.toString()
        val year = binding.etReleaseYear.text.toString()
        val gearbox = binding.actGearbox.text.toString()
        val drive = binding.actDrive.text.toString()
        val weight = binding.etWeightKg.text.toString()
        val powerHp = binding.etEnginePowerHp.text.toString()
        val powerKw = binding.etEnginePowerKw.text.toString()
        val engVolume = binding.etEngineCapacityL.text.toString()
        val tankVolume = binding.etTankCapacityL.text.toString()
        val fuelType = binding.actFuelType.text.toString()
        val VIN = binding.etVin.text.toString()
        val stateNumber = binding.etStateNumber.text.toString()

        if (brand.isBlank()) {
            binding.tilBrand.error = "Введите марку"
            isValid = false
        }
        else {
            binding.tilBrand.error = null
        }

        if (model.isBlank()) {
            binding.tilModel.error = "Введите модель"
            isValid = false
        }
        else {
            binding.tilModel.error = null
        }

        if (body.isBlank()) {
            binding.tilBody.error = "Введите кузов"
            isValid = false
        }
        else {
            binding.tilBody.error = null
        }

        if (year.isBlank()) {
            binding.tilReleaseYear.error = "Введите год выпуска"
            isValid = false
        }
        else if (year.toIntOrNull() == null) {
            binding.tilReleaseYear.error = "Год - целое число"
            isValid = false
        }
        else if (year.toInt() < 2000) {
            binding.tilReleaseYear.error = "Только автомобили с 2000 г.в."
            isValid = false
        }
        else if (year.toInt() > Year.now().value) {
            binding.tilReleaseYear.error = "Некорректный год выпуска"
            isValid = false
        }
        else {
            binding.tilReleaseYear.error = null
        }

        if (gearbox.isBlank()) {
            binding.tilGearbox.error = "Введите КПП"
            isValid = false
        }
        else {
            binding.tilGearbox.error = null
        }

        if (drive.isBlank()) {
            binding.tilDrive.error = "Введите привод"
            isValid = false
        }
        else {
            binding.tilDrive.error = null
        }

        if (weight.isBlank()) {
            binding.tilWeightKg.error = "Введите массу автомобиля"
            isValid = false
        }
        else if (weight.toIntOrNull() == null) {
            binding.tilWeightKg.error = "Масса - целое число"
            isValid = false
        }
        else if (weight.toInt() < 750) {
            binding.tilWeightKg.error = "Минимальная масса - 750 кг"
            isValid = false
        }
        else if (weight.toInt() > 5000) {
            binding.tilWeightKg.error = "Максимальная масса - 5000 кг"
            isValid = false
        }
        else {
            binding.tilWeightKg.error = null
        }

        if (powerHp.isBlank()) {
            binding.tilEnginePowerHp.error = "Введите мощность (л.с.)"
            isValid = false
        }
        else if (powerHp.toIntOrNull() == null) {
            binding.tilEnginePowerHp.error = "Мощность (л.с.) - целое число"
            isValid = false
        }
        else if (powerHp.toInt() < 60) {
            binding.tilEnginePowerHp.error = "Минимальная мощность - 60 л.с."
            isValid = false
        }
        else if (powerHp.toInt() > 5000) {
            binding.tilEnginePowerHp.error = "Максимальная мощность - 5000 л.с."
            isValid = false
        }
        else {
            binding.tilEnginePowerHp.error = null
        }

        if (powerKw.isBlank()) {
            binding.tilEnginePowerKw.error = "Введите мощность (кВт)"
            isValid = false
        }
        else if (powerKw.toFloat() < 2) {
            binding.tilEnginePowerKw.error = "Минимальная мощность - 2 кВт"
            isValid = false
        }
        else if (powerKw.toFloat() > 2000) {
            binding.tilEnginePowerKw.error = "Максимальная мощность - 2000 кВт"
            isValid = false
        }
        else {
            binding.tilEnginePowerKw.error = null
        }

        if (engVolume.isBlank()) {
            binding.tilEngineCapacityL.error = "Введите объём двигателя"
            isValid = false
        }
        else if (engVolume.toFloat() < 0.4) {
            binding.tilEngineCapacityL.error = "Минимальный объём - 0.4 л"
            isValid = false
        }
        else if (engVolume.toFloat() > 50) {
            binding.tilEngineCapacityL.error = "Максимальный объём - 50 л"
            isValid = false
        }
        else {
            binding.tilEngineCapacityL.error = null
        }

        if (tankVolume.isBlank()) {
            binding.tilTankCapacityL.error = "Введите объём бака"
            isValid = false
        }
        else if (tankVolume.toIntOrNull() == null) {
            binding.tilTankCapacityL.error = "Объём бака - целое число"
            isValid = false
        }
        else if (tankVolume.toInt() < 10) {
            binding.tilTankCapacityL.error = "Минимальный объём - 10 л"
            isValid = false
        }
        else if (tankVolume.toInt() > 250) {
            binding.tilTankCapacityL.error = "максимальный объём - 250 л"
            isValid = false
        }
        else {
            binding.tilTankCapacityL.error = null
        }

        if (fuelType.isBlank()) {
            binding.tilFuelType.error = "Введите топливо"
            isValid = false
        }
        else {
            binding.tilFuelType.error = null
        }

        val vinPattern = Regex("[A-Za-z0-9]{17}")
        if (VIN.isBlank()) {
            binding.tilVin.error = "Введите VIN"
            isValid = false
        }
        else if (VIN.length != 17) {
            binding.tilVin.error = "Длина VIN - 17 символов"
            isValid = false
        }
        else if (!vinPattern.matches(VIN)) {
            binding.tilVin.error = "VIN может содержать только цифры и символы латинского алфавита"
            isValid = false
        }
        else {
            binding.tilVin.error = null
        }

        if (!stateNumber.isBlank()) {
            val pattern = Regex("^[авекмнорстухАВЕКМНОРСТУХ][0-9]{3}[авекмнорстухАВЕКМНОРСТУХ]{2}[0-9]{2,3}$")

            if (!pattern.matches(stateNumber)) {
                binding.tilStateNumber.error = "Некорректный формат"
            }
            else {
                binding.tilStateNumber.error = null
            }
        }
        else {
            binding.tilStateNumber.error = null
        }

        binding.btnUpdate.isEnabled = !isValid

        if (isValid) {
            var file: MultipartBody.Part? = null

            if (selectedPhotoUri != null) {
                file = uriToMultipart(selectedPhotoUri!!, requireContext())
            }

            viewModel.updateCar(
                VIN, stateNumber.ifBlank { null },
                brand, model,
                body, year.toUShort(),
                gearbox, drive,
                weight.toUShort(), powerHp.toUShort(),
                powerKw.toFloat(), engVolume.toFloat(),
                tankVolume.toUByte(), fuelType,
                viewModel.selectedCarToDetail!!.carId, file
            )
        }
    }

    private fun uriToMultipart(uri: Uri, context: Context, partName: String = "file"): MultipartBody.Part? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream ?: return null

        val bytes = inputStream.readBytes()
        val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull(), 0, bytes.size)
        return MultipartBody.Part.createFormData(partName, "avatar.jpg", requestBody)
    }

    private fun loadCarPhoto(target: ShapeableImageView, photoId: UInt) {
        if (TokenStorage.getAccessToken().isNullOrBlank()) {
            target.setImageResource(R.drawable.ic_avatar_placeholder)
            return
        }

        val url = "${BuildConfig.BASE_URL}car-photos/${photoId}"

        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .into(target)
    }

    private fun startSignInActivity() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_SELECT_BRAND = "request_select_brand"
        const val KEY_BRAND = "brand"
        const val REQUEST_SELECT_MODEL = "request_select_model"
        const val KEY_MODEL = "model"
    }
}