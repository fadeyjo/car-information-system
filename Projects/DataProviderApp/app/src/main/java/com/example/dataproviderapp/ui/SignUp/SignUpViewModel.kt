package com.example.dataproviderapp.ui.SignUp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.SignUpRequest
import com.example.dataproviderapp.repositories.PersonsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class SignUpViewModel : ViewModel() {
    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(
        email: String, phone: String,
        lastName: String, firstName: String,
        patronymic: String?, birth: LocalDate,
        password: String, driveLicense: String?
    ) {
        viewModelScope.launch {
            _signUpState.value = SignUpState.Loading

            val body = SignUpRequest(
                email, phone,
                lastName, firstName,
                patronymic, birth,
                password, driveLicense,
                1u
            )

            val response = PersonsRepository.signUp(body)

            _signUpState.value = when (response) {
                is ApiResult.Error -> {
                    if (response.error == "Пользователь с данным email уже существует") {
                        SignUpState.PersonExistsByEmail
                    } else if (response.error == "Пользователь с данным номером телефона уже существует") {
                        SignUpState.PersonExistsByPhone
                    }
                    else if (response.error == "Пользователь с данным водительским удостоверением уже существует") {
                        SignUpState.PersonExistsByDriveLicense
                    } else {
                        SignUpState.UnknownError
                    }
                }
                ApiResult.NetworkError -> SignUpState.NetworkError
                is ApiResult.Success<*> -> SignUpState.Registered
                ApiResult.UnknownError -> SignUpState.UnknownError
                is ApiResult.ValidationError -> SignUpState.ValidationError(response.errors)
            }
        }
    }
}

sealed class SignUpState() {
    object Idle : SignUpState()
    object Loading : SignUpState()
    object Registered : SignUpState()
    data class ValidationError(
        val errors: List<Map<String, String>>
    ) : SignUpState()
    object PersonExistsByEmail : SignUpState()
    object PersonExistsByPhone : SignUpState()
    object PersonExistsByDriveLicense : SignUpState()
    object UnknownError : SignUpState()
    object NetworkError : SignUpState()
}