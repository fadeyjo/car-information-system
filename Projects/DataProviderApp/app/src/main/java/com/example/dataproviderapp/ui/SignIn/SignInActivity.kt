package com.example.dataproviderapp.ui.SignIn

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
import com.example.dataproviderapp.databinding.ActivitySignInBinding
import com.example.dataproviderapp.jwtutils.TokenStorage
import com.example.dataproviderapp.ui.Nav.NavActivity
import com.example.dataproviderapp.ui.SignUp.SignUpActivity
import kotlinx.coroutines.launch
import kotlin.getValue

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding

    private val viewModel: SignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSignIn)

        binding.btnSignIn.setOnClickListener {
            signIn()
        }

        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sign_in, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_up -> {
                startActivity(Intent(this, SignUpActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signIn() {
        viewModel.resetState()

        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

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

        binding.btnSignIn.isEnabled = !isValid

        if (isValid) {
            viewModel.signIn(email, password)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signInState.collect { state ->
                    when (state) {
                        SignInState.NetworkError -> showErrorDialog("Нет подключения к интернету")
                        SignInState.PersonNotFound -> binding.tilEmail.error = "Пользователь не найден"
                        is SignInState.Tokens -> {
                            TokenStorage.saveTokens(state.accessTokens, state.refreshToken)
                            moveToNavActivity()
                        }
                        SignInState.Unauthorized -> binding.tilPassword.error = "Неверный пароль"
                        SignInState.UnknownError -> showErrorDialog("Возникла неизвестная ошибка")
                        is SignInState.ValidationError -> {
                            state.errors.forEach { fieldErrorMap ->
                                fieldErrorMap.forEach { (field, message) ->
                                    when (field.lowercase()) {
                                        "email" -> binding.tilEmail.error = message
                                        "password" -> binding.tilPassword.error = message
                                    }
                                }
                            }
                        }

                        else -> {}
                    }

                    binding.btnSignIn.isEnabled = true;
                }
            }
        }
    }

    private fun moveToNavActivity() {
        startActivity(Intent(this, NavActivity::class.java))
        finish()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК", null)
            .show()
    }
}