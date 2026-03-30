package com.example.dataproviderapp.ui.Nav.Fragments.Profile

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.manager.Lifecycle
import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.FragmentUpdatePersonBinding
import com.example.dataproviderapp.jwtutils.TokenStorage
import com.example.dataproviderapp.ui.Nav.NavViewModel
import com.example.dataproviderapp.ui.Nav.UpdatePersonState
import com.example.dataproviderapp.ui.SignIn.SignInActivity
import com.example.dataproviderapp.ui.SignUp.SignUpState
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
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.getValue

class UpdatePersonFragment : Fragment() {
    private var _binding: FragmentUpdatePersonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    private var selectedAvatarUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedAvatarUri = it
                binding.ivProfileAvatar.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUpdatePersonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val person = viewModel.person

        binding.etEmail.setText(person.email)
        binding.etPhone.setText(person.phone)
        binding.etLastName.setText(person.lastName)
        binding.etFirstName.setText(person.firstName)
        if (!person.patronymic.isNullOrEmpty()) {
            binding.etPatronymic.setText(person.patronymic)
        }
        val birthFormatted = person.birth.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        binding.etBirthDate.setText(birthFormatted)
        if (!person.driveLicense.isNullOrEmpty()) {
            binding.etDriverLicense.setText(person.driveLicense)
        }

        loadAvatarIntoProfile(binding.ivProfileAvatar, person.avatarId)

        binding.etBirthDate.setOnClickListener {
            showBirthDatePicker(binding.etBirthDate)
        }

        binding.ivProfileAvatar.setOnClickListener {
            pickImageFromGallery()
        }

        binding.btnUpdate.setOnClickListener {
            redactData()
        }

        observeVieModel()
    }

    private fun observeVieModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.updatePersonState.collect { state ->
                    when (state) {
                        UpdatePersonState.CannotClearDriveLicense -> binding.tilDriverLicense.error = "У вас имеются автомобили"
                        UpdatePersonState.NetworkError -> showErrorDialog("Нет подключения к интрнету")
                        UpdatePersonState.PersonExistsByDriveLicense -> binding.tilDriverLicense.error = "Пользователь с таким ВУ уже существует"
                        UpdatePersonState.PersonExistsByEmail -> binding.tilEmail.error = "Пользователь с таким email уже существует"
                        UpdatePersonState.PersonExistsByPhone -> binding.tilPhone.error = "Пользователь с таким номером телефона уже существует уже существует"
                        UpdatePersonState.PersonNotFound -> moveToSignIn()
                        UpdatePersonState.SomeErrorToCreateAvatar -> showErrorDialog("Ошибка при создании аватара")
                        UpdatePersonState.UnknownError -> showErrorDialog("Неизвестная ошибка")
                        UpdatePersonState.Updated -> {
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                        is UpdatePersonState.ValidationError -> {
                            state.errors.forEach { fieldErrorMap ->
                                fieldErrorMap.forEach { (field, message) ->
                                    when (field.lowercase()) {
                                        "email" -> binding.tilEmail.error = message
                                        "phone" -> binding.tilPhone.error = message
                                        "drivelicense" -> binding.tilDriverLicense.error = message
                                        "lastname" -> binding.tilLastName.error = message
                                        "firstname" -> binding.tilFirstName.error = message
                                        "patronymic" -> binding.tilPatronymic.error = message
                                        "birth" -> binding.tilBirthDate.error = message
                                    }
                                }
                            }
                        }

                        else -> {}
                    }

                    binding.btnUpdate.isEnabled = true
                }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК", null)
            .show()
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun redactData() {
        viewModel.resetUpdatePersonState()

        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val lastName = binding.etLastName.text.toString()
        val firstName = binding.etFirstName.text.toString()
        val patronymic = binding.etPatronymic.text.toString()
        val birth = binding.etBirthDate.text.toString()
        val driveLicense = binding.etDriverLicense.text.toString()

        var isValid = true

        if (email.isBlank()) {
            binding.tilEmail.error = "Введите email"
            isValid = false
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Некорректный формат email"
            isValid = false
        }
        else {
            binding.tilEmail.error = null
        }

        val phoneRegex1 = Regex("""^\+7\d*$""")
        val phoneRegex2 = Regex("""^\+7\d{10}$""")
        if (phone.isBlank()) {
            binding.tilPhone.error = "Введите номер телефона"
            isValid = false
        }
        else if (!phoneRegex1.matches(phone)) {
            binding.tilPhone.error = "Номер телефона должен состоять из +7 и цифр"
            isValid = false
        }
        else if (!phoneRegex2.matches(phone)) {
            binding.tilPhone.error = "Номер телефона должен содержать 11 цифр"
            isValid = false
        }
        else {
            binding.tilPhone.error = null
        }

        val namesRegex1 = Regex("""^[А-ЯЁа-яё]*$""")
        val namesRegex2 = Regex("""^[А-ЯЁ][а-яё]*$""")
        val namesRegex3 = Regex("""^[А-ЯЁ][а-яё]{1,49}$""")

        if (lastName.isBlank()) {
            binding.tilLastName.error = "Введите фамилию"
            isValid = false
        }
        else if (!namesRegex1.matches(lastName)) {
            binding.tilLastName.error = "Фамилия должна состоять только из букв"
            isValid = false
        }
        else if (!namesRegex2.matches(lastName)) {
            binding.tilLastName.error = "Первая буква - строчная, остальные - прописные"
            isValid = false
        }
        else if (!namesRegex3.matches(lastName)) {
            binding.tilLastName.error = "Минимальная длина 2, максимальная - 50"
            isValid = false
        }
        else {
            binding.tilLastName.error = null
        }

        if (firstName.isBlank()) {
            binding.tilFirstName.error = "Введите имя"
            isValid = false
        }
        else if (!namesRegex1.matches(firstName)) {
            binding.tilFirstName.error = "Имя должно состоять только из букв"
            isValid = false
        }
        else if (!namesRegex2.matches(firstName)) {
            binding.tilFirstName.error = "Первая буква - строчная, остальные - прописные"
            isValid = false
        }
        else if (!namesRegex3.matches(firstName)) {
            binding.tilFirstName.error = "Минимальная длина 2, максимальная - 50"
            isValid = false
        }
        else {
            binding.tilFirstName.error = null
        }

        if (!patronymic.isBlank()) {
            if (!namesRegex1.matches(patronymic)) {
                binding.tilPatronymic.error = "Отчество должно состоять только из букв"
                isValid = false
            }
            else if (!namesRegex2.matches(patronymic)) {
                binding.tilPatronymic.error = "Первая буква - строчная, остальные - прописные"
                isValid = false
            }
            else if (!namesRegex3.matches(patronymic)) {
                binding.tilPatronymic.error = "Минимальная длина 2, максимальная - 50"
                isValid = false
            }
            else {
                binding.tilPatronymic.error = null
            }
        }
        else {
            binding.tilPatronymic.error = null
        }

        if (birth.isBlank()) {
            binding.tilBirthDate.error = "Введите дату рождения"
            isValid = false
        }
        else {
            binding.tilBirthDate.error = null
        }

        val driveLicenseRegex1 = Regex("""^\d*$""")
        val driveLicenseRegex2 = Regex("""^\d{10}$""")
        if (birth.isBlank()) {
            binding.tilDriverLicense.error = "Введите водительское удостоверение"
            isValid = false
        }
        else if (!driveLicenseRegex1.matches(driveLicense)) {
            binding.tilDriverLicense.error = "Номер ВУ должен состоять только из цифр"
            isValid = false
        }
        else if (!driveLicenseRegex2.matches(driveLicense)) {
            binding.tilDriverLicense.error = "Номер ВУ должен состоять из 10 цифр"
            isValid = false
        }
        else {
            binding.tilDriverLicense.error = null
        }

        binding.btnUpdate.isEnabled = !isValid

        if (isValid) {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val birthDate = LocalDate.parse(birth, formatter)

            var file: MultipartBody.Part? = null

            if (selectedAvatarUri != null) {
                file = uriToMultipart(selectedAvatarUri!!, requireContext())
            }

            viewModel.updatePerson(
                email, phone,
                lastName, firstName,
                patronymic.ifBlank { null }, birthDate,
                driveLicense, file
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

    private fun loadAvatarIntoProfile(target: ShapeableImageView, avatarId: UInt) {
        if (TokenStorage.getAccessToken().isNullOrBlank()) {
            target.setImageResource(R.drawable.ic_avatar_placeholder)
            return
        }

        val url = "${BuildConfig.BASE_URL}avatars/${avatarId}"

        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .into(target)
    }

    private fun showBirthDatePicker(birthDateInput: TextInputEditText) {
        val today = Calendar.getInstance()
        val maxDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, -18)
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                birthDateInput.setText(dateFormat.format(selected.time))
            },
            maxDate.get(Calendar.YEAR),
            maxDate.get(Calendar.MONTH),
            maxDate.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        datePickerDialog.datePicker.minDate = Calendar.getInstance().apply {
            set(1900, Calendar.JANUARY, 1)
        }.timeInMillis
        datePickerDialog.show()
    }

    private fun moveToSignIn() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}