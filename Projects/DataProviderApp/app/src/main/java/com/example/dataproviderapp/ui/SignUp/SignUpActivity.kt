package com.example.dataproviderapp.ui.SignUp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.ActivitySignUpBinding
import com.example.dataproviderapp.ui.SignIn.SignInActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSignUp)

        binding.etBirthDate.setOnClickListener {
            showBirthDatePicker(binding.etBirthDate)
        }

        binding.btnSignUp.setOnClickListener {
            signUp()
        }

        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sign_up, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_in -> {
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signUpState.collect { state ->
                    when (state) {
                        SignUpState.NetworkError -> showErrorDialog("Нет подключения к интернету")
                        SignUpState.PersonExistsByDriveLicense -> {
                            binding.tilDriverLicense.error = "Пользователь с таким ВУ уже существует"
                        }
                        SignUpState.PersonExistsByEmail -> {
                            binding.tilEmail.error = "Пользователь с таким email уже существует"
                        }
                        SignUpState.PersonExistsByPhone -> {
                            binding.tilPhone.error = "Пользователь с таким номером телефона уже существует"
                        }
                        SignUpState.Registered -> {
                            AlertDialog.Builder(this@SignUpActivity)
                                .setTitle("Информация")
                                .setMessage("Вы успешно зарегистрированы! Необходим вход.")
                                .setCancelable(false)
                                .setPositiveButton("ОК") {_, _ ->
                                    signIn()
                                    finish()
                                }
                                .show()
                        }
                        SignUpState.UnknownError -> showErrorDialog("Возникла неизвестная ошибка")
                        is SignUpState.ValidationError -> {
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
                                        "password" -> binding.tilPassword.error = message
                                    }
                                }
                            }
                        }

                        else -> {}
                    }

                    binding.btnSignUp.isEnabled = true
                }
            }
        }
    }

    private fun signUp() {
        viewModel.resetState()

        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val lastName = binding.etLastName.text.toString()
        val firstName = binding.etFirstName.text.toString()
        val patronymic = binding.etPatronymic.text.toString()
        val birth = binding.etBirthDate.text.toString()
        val driveLicense = binding.etDriverLicense.text.toString()
        val password = binding.etPassword.text.toString()
        val repeatPassword = binding.etRepeatPassword.text.toString()

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

        if (password.isBlank()) {
            binding.tilPassword.error = "Введите пароль"
            isValid = false
        }
        else if (password.length < 8) {
            binding.tilPassword.error = "Минимальная длина пароля - 8 символов"
            isValid = false
        }
        else if (password.length > 32) {
            binding.tilPassword.error = "Максимальная длина пароля - 32 символа"
            isValid = false
        }
        else {
            binding.tilPassword.error = null
        }

        if (binding.tilPassword.error == null)
        {
            if (repeatPassword.isBlank()) {
                binding.tilRepeatPassword.error = "Повторите пароль"
                isValid = false
            }
            else if (password != repeatPassword) {
                binding.tilRepeatPassword.error = "Пароли не совпадают"
                isValid = false
            }
            else {
                binding.tilRepeatPassword.error = null
            }
        }
        else {
            binding.tilRepeatPassword.error = null
        }

        binding.btnSignUp.isEnabled = !isValid

        if (isValid) {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val birthDate = LocalDate.parse(birth, formatter)

            viewModel.signUp(
                email, phone,
                lastName, firstName,
                patronymic.ifBlank { null }, birthDate,
                password, driveLicense
            )
        }
    }

    private fun showBirthDatePicker(birthDateInput: TextInputEditText) {
        val today = Calendar.getInstance()
        val maxDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, -18)
        }

        val datePickerDialog = DatePickerDialog(
            this,
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

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК", null)
            .show()
    }

    private fun signIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
}