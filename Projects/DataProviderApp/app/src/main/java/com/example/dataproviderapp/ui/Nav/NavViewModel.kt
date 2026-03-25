package com.example.dataproviderapp.ui.Nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.CreateCarRequest
import com.example.dataproviderapp.dto.requests.UpdateCarInfoRequest
import com.example.dataproviderapp.dto.requests.UpdatePersonInfoRequest
import com.example.dataproviderapp.dto.responses.CarDto
import com.example.dataproviderapp.dto.responses.PersonDto
import com.example.dataproviderapp.repositories.AuthRepository
import com.example.dataproviderapp.repositories.CarsRepository
import com.example.dataproviderapp.repositories.PersonsRepository
import com.example.dataproviderapp.ui.Nav.ProfileDataState.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class NavViewModel : ViewModel() {
    private val _profileDataState = MutableStateFlow<ProfileDataState>(ProfileDataState.Idle)
    val profileDataState: StateFlow<ProfileDataState> = _profileDataState

    private val _carsState = MutableStateFlow<CarsState>(CarsState.Idle)
    val carsState: StateFlow<CarsState> = _carsState

    private val _updatePersonState = MutableStateFlow<UpdatePersonState>(UpdatePersonState.Idle)
    val updatePersonState: StateFlow<UpdatePersonState> = _updatePersonState

    private val _updateCarState = MutableStateFlow<UpdateCarState>(UpdateCarState.Idle)
    val updateCarState: StateFlow<UpdateCarState> = _updateCarState

    private val _createCarState = MutableStateFlow<CreateCarState>(CreateCarState.Idle)
    val createCarState: StateFlow<CreateCarState> = _createCarState

    private val _logoutState = MutableStateFlow<LogOutState>(LogOutState.Idle)
    val logoutState: StateFlow<LogOutState> = _logoutState


    fun getPersonData() {
        viewModelScope.launch {
            _profileDataState.value = ProfileDataState.Loading

            val response = PersonsRepository.getPersonById()

            _profileDataState.value = when (response) {
                is ApiResult.Error -> {
                    if (response.code == 404) {
                        ProfileDataState.PersonNotFound
                    } else {
                        ProfileDataState.UnknownError
                    }
                }
                ApiResult.NetworkError -> ProfileDataState.NetworkError
                is ApiResult.Success -> {
                    if (response.data == null) {
                        ProfileDataState.UnknownError
                    } else {
                        Person(response.data)
                    }
                }
                else -> ProfileDataState.NetworkError
            }
        }
    }

    fun getPersonCars() {
        viewModelScope.launch {
            _carsState.value = CarsState.Loading

            val response = CarsRepository.getCarsByPersonId()

            _carsState.value = when (response) {
                is ApiResult.Error -> {
                    if (response.code == 404) {
                        CarsState.PersonNotFound
                    } else {
                        CarsState.UnknownError
                    }
                }
                ApiResult.NetworkError -> CarsState.NetworkError
                is ApiResult.Success -> {
                    if (response.data == null) {
                        CarsState.UnknownError
                    } else {
                        CarsState.Cars(response.data)
                    }
                }
                else -> CarsState.UnknownError
            }
        }
    }

    fun updatePerson(
        email: String, phone: String,
        lastName: String, firstName: String,
        patronymic: String?, birth: LocalDate,
        driveLicense: String?
    ) {
        viewModelScope.launch {
            _updatePersonState.value = UpdatePersonState.Idle

            val body = UpdatePersonInfoRequest(
                email, phone,
                lastName, firstName,
                patronymic, birth,
                driveLicense
            )

            val response = PersonsRepository.updatePersonInfo(body)

            _updatePersonState.value = when (response) {
                is ApiResult.Error -> {
                    if (response.code == 404) {
                        UpdatePersonState.PersonNotFound
                    }
                    else if (response.code == 409) {
                        if (response.error == "Пользователь с данным email уже существует") {
                            UpdatePersonState.PersonExistsByEmail
                        } else if (response.error == "Пользователь с данным номером телефона уже существует") {
                            UpdatePersonState.PersonExistsByPhone
                        } else if (response.error == "Пользователь с данным водительским удостоверением уже существует") {
                            UpdatePersonState.PersonExistsByDriveLicense
                        } else {
                            UpdatePersonState.UnknownError
                        }
                    } else {
                        UpdatePersonState.UnknownError
                    }
                }
                ApiResult.NetworkError -> UpdatePersonState.NetworkError
                is ApiResult.Success<*> -> UpdatePersonState.Updated
                ApiResult.UnknownError -> UpdatePersonState.UnknownError
                is ApiResult.ValidationError -> UpdatePersonState.ValidationError(response.errors)
            }
        }
    }

    fun updateCar(
        vinNumber: String, stateNumber: String?,
        brandModel: String, modelName: String,
        bodyName: String, releaseYear: UShort,
        gearboxName: String, driveName: String,
        vehicleWeightKg: UShort, enginePowerHw: UShort,
        enginePowerKw: Float, engineCapacityL: Float,
        tankCapacityL: UByte, fuelTypeName: String,
        carId: UInt
    ) {
        viewModelScope.launch {
            _updateCarState.value = UpdateCarState.Loading

            val body = UpdateCarInfoRequest(
                vinNumber, stateNumber,
                brandModel, modelName,
                bodyName, releaseYear,
                gearboxName, driveName,
                vehicleWeightKg, enginePowerHw,
                enginePowerKw, engineCapacityL,
                tankCapacityL, fuelTypeName
            )

            val response = CarsRepository.updateCarInfo(carId, body)

            _updateCarState.value = when (response) {
                is ApiResult.Error ->{
                    if (response.code == 404) {
                        if (response.error == "Автомобиль не найден") {
                            UpdateCarState.CarNotFound
                        } else {
                            UpdateCarState.UnknownError
                        }
                    } else if (response.code == 409) {
                        if (response.error == "Автомобиль с данным VIN уже существует") {
                            UpdateCarState.CarExistsByVin
                        } else if (response.error == "Автомобиль с данным гос. номером уже существует") {
                            UpdateCarState.CarExistsByStateNumber
                        } else {
                            UpdateCarState.UnknownError
                        }
                    } else {
                        UpdateCarState.UnknownError
                    }
                }
                ApiResult.NetworkError -> UpdateCarState.NetworkError
                is ApiResult.Success<*> -> UpdateCarState.Updated
                ApiResult.UnknownError -> UpdateCarState.UnknownError
                is ApiResult.ValidationError -> UpdateCarState.ValidationError(response.errors)
            }
        }
    }

    fun createCar(
        vinNumber: String, stateNumber: String?,
        brandModel: String, modelName: String,
        bodyName: String, releaseYear: UShort,
        gearboxName: String, driveName: String,
        vehicleWeightKg: UShort, enginePowerHw: UShort,
        enginePowerKw: Float, engineCapacityL: Float,
        tankCapacityL: UByte, fuelTypeName: String
    ) {
        viewModelScope.launch {
            _createCarState.value = CreateCarState.Loading

            val body = CreateCarRequest(
                vinNumber, stateNumber,
                brandModel, modelName,
                bodyName, releaseYear,
                gearboxName, driveName,
                vehicleWeightKg, enginePowerHw,
                enginePowerKw, engineCapacityL,
                tankCapacityL, fuelTypeName
            )

            val response = CarsRepository.createCar(body)

            _createCarState.value = when (response) {
                is ApiResult.Error -> {
                    if (response.code == 404) {
                        CreateCarState.PersonNotFound
                    }
                    else if (response.code == 409) {
                        if (response.error == "Регистрация автомобиля невозможна, так как у вас отсутствует водительское удостоверение") {
                            CreateCarState.CannotCreateCar
                        } else if (response.error == "Автомобиль с данным VIN уже существует") {
                            CreateCarState.CarExistsByVin
                        } else if (response.error == "Автомобиль с данным гос. номером уже существует") {
                            CreateCarState.CarExistsByStateNumber
                        } else {
                            CreateCarState.UnknownError
                        }
                    } else {
                        CreateCarState.UnknownError
                    }
                }
                ApiResult.NetworkError -> CreateCarState.NetworkError
                is ApiResult.Success<*> -> CreateCarState.Created
                ApiResult.UnknownError -> CreateCarState.UnknownError
                is ApiResult.ValidationError -> CreateCarState.ValidationError(response.errors)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = LogOutState.Loading

            val response = AuthRepository.logOut()

            _logoutState.value = when (response) {
                is ApiResult.Success<*> -> LogOutState.LogOuted

                else -> LogOutState.SomeError
            }
        }
    }
}

sealed class ProfileDataState {
    object Idle : ProfileDataState()
    object Loading : ProfileDataState()
    object UnknownError : ProfileDataState()
    object NetworkError : ProfileDataState()
    object PersonNotFound : ProfileDataState()
    data class Person(
        val person: PersonDto
    ) : ProfileDataState()
}

sealed class CarsState {
    object Idle : CarsState()
    object Loading : CarsState()
    object UnknownError : CarsState()
    object NetworkError : CarsState()
    object PersonNotFound : CarsState()
    data class Cars(
        val cars: List<CarDto>
    ) : CarsState()
}

sealed class UpdatePersonState {
    object Idle : UpdatePersonState()
    object Loading : UpdatePersonState()
    object UnknownError : UpdatePersonState()
    object NetworkError : UpdatePersonState()
    object PersonNotFound : UpdatePersonState()
    object CannotClearDriveLicense: UpdatePersonState()
    object PersonExistsByEmail : UpdatePersonState()
    object PersonExistsByPhone : UpdatePersonState()
    object PersonExistsByDriveLicense : UpdatePersonState()
    data class ValidationError(
        val errors: List<Map<String, String>>
    ) : UpdatePersonState()
    object Updated : UpdatePersonState()
}

sealed class UpdateCarState {
    object Idle : UpdateCarState()
    object Loading : UpdateCarState()
    object UnknownError : UpdateCarState()
    object NetworkError : UpdateCarState()
    object CarNotFound : UpdateCarState()
    object CarExistsByVin : UpdateCarState()
    object CarExistsByStateNumber : UpdateCarState()
    data class ValidationError(
        val errors: List<Map<String, String>>
    ) : UpdateCarState()
    object Updated : UpdateCarState()
}

sealed class CreateCarState {
    object Idle : CreateCarState()
    object Loading : CreateCarState()
    object UnknownError : CreateCarState()
    object NetworkError : CreateCarState()
    object PersonNotFound : CreateCarState()
    object CannotCreateCar : CreateCarState()
    object CarExistsByVin : CreateCarState()
    object CarExistsByStateNumber : CreateCarState()
    data class ValidationError(
        val errors: List<Map<String, String>>
    ) : CreateCarState()
    object Created : CreateCarState()
}

sealed class LogOutState {
    object Idle : LogOutState()
    object Loading : LogOutState()
    object SomeError : LogOutState()
    object LogOuted: LogOutState()
}