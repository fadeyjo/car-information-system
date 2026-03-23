package com.example.dataproviderapp.ui.Main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.repositories.PersonsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _personState = MutableStateFlow<PersonState>(PersonState.Idle)
    val personState: StateFlow<PersonState> = _personState

    fun checkPersonAuthorized() {
        viewModelScope.launch {
            _personState.value = PersonState.Loading

            val personData = PersonsRepository.getPersonById()

            _personState.value = when (personData) {
                is ApiResult.Success<*> -> PersonState.Authorized
                else -> PersonState.Unauthorized
            }
        }
    }
}


sealed class PersonState {
    object Idle : PersonState()
    object Loading : PersonState()
    object Authorized : PersonState()
    object  Unauthorized : PersonState()
}