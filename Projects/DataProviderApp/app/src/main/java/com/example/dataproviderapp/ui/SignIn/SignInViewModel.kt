package com.example.dataproviderapp.ui.SignIn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.SignInRequest
import com.example.dataproviderapp.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _signInState.value = SignInState.Idle

            val body = SignInRequest(email, password)

            val response = AuthRepository.logIn(body)

            _signInState.value = when (response) {
                is ApiResult.Error -> {
                    if (response.code == 404) {
                        SignInState.PersonNotFound
                    }
                    else {
                        SignInState.Unauthorized
                    }
                }
                ApiResult.NetworkError -> SignInState.NetworkError
                is ApiResult.Success -> {
                    if (response.data == null) {
                        SignInState.UnknownError
                    } else {
                        SignInState.Tokens(
                            response.data.accessToken,
                            response.data.refreshToken
                        )
                    }
                }
                ApiResult.UnknownError -> SignInState.UnknownError
                is ApiResult.ValidationError -> SignInState.ValidationError(response.errors)
            }
        }
    }
}

sealed class SignInState() {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Tokens(
        val accessTokens: String,
        val refreshToken: String
    ) : SignInState()
    object Unauthorized : SignInState()
    object PersonNotFound : SignInState()
    data class ValidationError(
        val errors: List<Map<String, String>>
    ) : SignInState()
    object UnknownError : SignInState()
    object NetworkError : SignInState()
}