package com.example.dataproviderapp.ui.Nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dataproviderapp.apiutils.ApiResult
import com.example.dataproviderapp.dto.requests.CreateCarRequest
import com.example.dataproviderapp.dto.requests.StartTripRequest
import com.example.dataproviderapp.dto.requests.UpdateCarInfoRequest
import com.example.dataproviderapp.dto.requests.UpdatePersonInfoRequest
import com.example.dataproviderapp.dto.responses.CarDto
import com.example.dataproviderapp.dto.responses.PersonDto
import com.example.dataproviderapp.dto.responses.TripDto
import com.example.dataproviderapp.repositories.AuthRepository
import com.example.dataproviderapp.repositories.AvatarsRepository
import com.example.dataproviderapp.repositories.CarBodiesRepository
import com.example.dataproviderapp.repositories.CarBrandsRepository
import com.example.dataproviderapp.repositories.CarDrivesRepository
import com.example.dataproviderapp.repositories.CarGearboxesRepository
import com.example.dataproviderapp.repositories.CarModelsRepository
import com.example.dataproviderapp.repositories.CarPhotosRepository
import com.example.dataproviderapp.repositories.CarsRepository
import com.example.dataproviderapp.repositories.FuelTypesRepository
import com.example.dataproviderapp.repositories.PersonsRepository
import com.example.dataproviderapp.repositories.TripsRepository
import com.example.dataproviderapp.ui.Nav.ProfileDataState.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.time.LocalDate
import java.time.LocalDateTime

class NavViewModel : ViewModel() {
    lateinit var person: PersonDto
    var selectedCar: CarDto? = null
    var currentTrip: TripDto? = null

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

    private val _checkBoxesDataState = MutableStateFlow<CheckBoxesDataState>(CheckBoxesDataState.Idle)
    val checkBoxesDataState: StateFlow<CheckBoxesDataState> = _checkBoxesDataState

    private val _brandsState = MutableStateFlow<BrandsState>(BrandsState.Idle)
    val brandsState: StateFlow<BrandsState> = _brandsState

    private val _modelsState = MutableStateFlow<ModelsState>(ModelsState.Idle)
    val modelsState: StateFlow<ModelsState> = _modelsState

    private val _startTripState = MutableStateFlow<StartTripState>(StartTripState.Idle)
    val startTripState: StateFlow<StartTripState> = _startTripState


    fun getPersonData() {
        viewModelScope.launch {
            resetUpdatePersonState()
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
                        person = response.data
                        Person(response.data)
                    }
                }
                else -> ProfileDataState.NetworkError
            }
        }
    }

    fun getPersonCars() {
        viewModelScope.launch {
            resetCreateCarState()
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
        driveLicense: String?, file: MultipartBody.Part?
    ) {
        viewModelScope.launch {
            _updatePersonState.value = UpdatePersonState.Loading

            val body = UpdatePersonInfoRequest(
                email, phone,
                lastName, firstName,
                patronymic, birth,
                driveLicense
            )

            val response = PersonsRepository.updatePersonInfo(body)

            when (response) {
                is ApiResult.Error -> {
                    if (response.code == 404) {
                        _updatePersonState.value = UpdatePersonState.PersonNotFound
                    }
                    else if (response.code == 409) {
                        if (response.error == "Пользователь с данным email уже существует") {
                            _updatePersonState.value = UpdatePersonState.PersonExistsByEmail
                        } else if (response.error == "Пользователь с данным номером телефона уже существует") {
                            _updatePersonState.value = UpdatePersonState.PersonExistsByPhone
                        } else if (response.error == "Пользователь с данным водительским удостоверением уже существует") {
                            _updatePersonState.value = UpdatePersonState.PersonExistsByDriveLicense
                        } else if (response.error == "Вы не можете очистить информацию о водительском удостоверении, так как у вас имеются автомобили") {
                            _updatePersonState.value = UpdatePersonState.CannotClearDriveLicense
                        } else {
                            _updatePersonState.value = UpdatePersonState.UnknownError
                        }
                    } else {
                        _updatePersonState.value = UpdatePersonState.UnknownError
                    }
                }
                ApiResult.NetworkError -> _updatePersonState.value = UpdatePersonState.NetworkError
                is ApiResult.Success<*> -> {}
                ApiResult.UnknownError -> _updatePersonState.value = UpdatePersonState.UnknownError
                is ApiResult.ValidationError -> _updatePersonState.value = UpdatePersonState.ValidationError(response.errors)
            }

            if (_updatePersonState.value != UpdatePersonState.Loading)
                return@launch

            if (file == null) {
                _updatePersonState.value = UpdatePersonState.Updated
                return@launch
            }

            val responseAvatar = AvatarsRepository.createAvatar(file)

            _updatePersonState.value = when (responseAvatar) {
                is ApiResult.Success<*> -> UpdatePersonState.Updated

                else -> UpdatePersonState.SomeErrorToCreateAvatar
            }
        }
    }

    fun resetUpdatePersonState() {
        _updatePersonState.value = UpdatePersonState.Idle
    }

    fun updateCar(
        vinNumber: String, stateNumber: String?,
        brandModel: String, modelName: String,
        bodyName: String, releaseYear: UShort,
        gearboxName: String, driveName: String,
        vehicleWeightKg: UShort, enginePowerHw: UShort,
        enginePowerKw: Float, engineCapacityL: Float,
        tankCapacityL: UByte, fuelTypeName: String,
        carId: UInt,  file: MultipartBody.Part?
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

            when (response) {
                is ApiResult.Error ->{
                    if (response.code == 404) {
                        if (response.error == "Автомобиль не найден") {
                            _updateCarState.value = UpdateCarState.CarNotFound
                        } else {
                            _updateCarState.value = UpdateCarState.UnknownError
                        }
                    } else if (response.code == 409) {
                        if (response.error == "Автомобиль с данным VIN уже существует") {
                            _updateCarState.value = UpdateCarState.CarExistsByVin
                        } else if (response.error == "Автомобиль с данным гос. номером уже существует") {
                            _updateCarState.value = UpdateCarState.CarExistsByStateNumber
                        } else {
                            _updateCarState.value = UpdateCarState.UnknownError
                        }
                    } else {
                        _updateCarState.value = UpdateCarState.UnknownError
                    }
                }
                ApiResult.NetworkError -> _updateCarState.value = UpdateCarState.NetworkError
                ApiResult.UnknownError -> _updateCarState.value = UpdateCarState.UnknownError
                is ApiResult.ValidationError -> _updateCarState.value = UpdateCarState.ValidationError(response.errors)

                else -> {}
            }

            if (_updateCarState.value != UpdateCarState.Loading) {
                return@launch
            }

            if (file == null) {
                val newCar = getCarAfterUpdating(carId)
                if (newCar == null) {
                    return@launch
                }
                selectedCar = newCar
                _updateCarState.value = UpdateCarState.Updated
                return@launch
            }

            val responsePhoto = CarPhotosRepository.createCarPhoto(carId, file)

            _updateCarState.value = when (responsePhoto) {
                is ApiResult.Success<*> -> {
                    val newCar = getCarAfterUpdating(carId)
                    if (newCar == null) {
                        return@launch
                    }
                    selectedCar = newCar
                    UpdateCarState.Updated
                }

                else -> UpdateCarState.SomeErrorToCreatePhoto
            }
        }
    }

    private suspend fun getCarAfterUpdating(carId: UInt): CarDto? {
        val response = CarsRepository.getCarById(carId)
        return when (response) {
            ApiResult.NetworkError -> {
                _updateCarState.value = UpdateCarState.NetworkError
                null
            }
            is ApiResult.Success -> response.data
            ApiResult.UnknownError -> {
                _updateCarState.value = UpdateCarState.UnknownError
                null
            }

            else -> {
                _updateCarState.value = UpdateCarState.UnknownError
                null
            }
        }
    }

    fun createCar(
        vinNumber: String, stateNumber: String?,
        brandName: String, modelName: String,
        bodyName: String, releaseYear: UShort,
        gearboxName: String, driveName: String,
        vehicleWeightKg: UShort, enginePowerHp: UShort,
        enginePowerKw: Float, engineCapacityL: Float,
        tankCapacityL: UByte, fuelTypeName: String
    ) {
        viewModelScope.launch {
            _createCarState.value = CreateCarState.Loading

            val body = CreateCarRequest(
                vinNumber, stateNumber,
                brandName, modelName,
                bodyName, releaseYear,
                gearboxName, driveName,
                vehicleWeightKg, enginePowerHp,
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

    fun getCheckboxesData() {
        viewModelScope.launch {
            _checkBoxesDataState.value = CheckBoxesDataState.Loading

            val resBodies = CarBodiesRepository.getAllBodies()

            val bodies: List<String>? = when (resBodies) {
                is ApiResult.Success -> {
                    resBodies.data?.map{ it.bodyName }
                }
                else -> null
            }

            if (bodies == null) {
                _checkBoxesDataState.value = CheckBoxesDataState.SomeError
                return@launch
            }

            val resGearboxes = CarGearboxesRepository.getAllGearboxes()

            val gearboxes: List<String>? = when (resGearboxes) {
                is ApiResult.Success -> {
                    resGearboxes.data?.map{ it.gearboxName }
                }
                else -> null
            }

            if (gearboxes == null) {
                _checkBoxesDataState.value = CheckBoxesDataState.SomeError
                return@launch
            }

            val resDrives = CarDrivesRepository.getAllDrives()

            val drives: List<String>? = when (resDrives) {
                is ApiResult.Success -> {
                    resDrives.data?.map{ it.driveName }
                }
                else -> null
            }

            if (drives == null) {
                _checkBoxesDataState.value = CheckBoxesDataState.SomeError
                return@launch
            }

            val resFuelTypes = FuelTypesRepository.getAllFuelTypes()

            val fuelTypes: List<String>? = when (resFuelTypes) {
                is ApiResult.Success -> {
                    resFuelTypes.data?.map{ it.typeName }
                }
                else -> null
            }

            if (fuelTypes == null) {
                _checkBoxesDataState.value = CheckBoxesDataState.SomeError
                return@launch
            }

            _checkBoxesDataState.value = CheckBoxesDataState.Data(
                bodies, gearboxes, drives, fuelTypes
            )
        }
    }

    fun getBrandsByText(text: String) {
        viewModelScope.launch {
            _brandsState.value = BrandsState.Loading

            val res = CarBrandsRepository.getAllBrandsByText(text)

            _brandsState.value = when (res) {
                is ApiResult.Success -> {
                    if (res.data != null) {
                        BrandsState.Data(res.data.map { it.brandName })
                    } else {
                        BrandsState.SomeError
                    }
                }

                else -> BrandsState.SomeError
            }
        }
    }

    fun getModelsByText(brandName: String, text: String) {
        viewModelScope.launch {
            _modelsState.value = ModelsState.Loading

            val res = CarModelsRepository.getAllBrandsByText(brandName, text)

            _modelsState.value = when (res) {
                is ApiResult.Success -> {
                    if (res.data != null) {
                        ModelsState.Data(res.data.map { it.modelName })
                    } else {
                        ModelsState.SomeError
                    }
                }

                else -> ModelsState.SomeError
            }
        }
    }

    fun resetCreateCarState() {
        _createCarState.value = CreateCarState.Idle
    }

    fun getAllBrands() {
        viewModelScope.launch {
            _brandsState.value = BrandsState.Loading

            val res = CarBrandsRepository.getAllBrands()

            _brandsState.value = when (res) {
                is ApiResult.Success -> {
                    if (res.data != null) {
                        BrandsState.Data(res.data.map { it.brandName })
                    } else {
                        BrandsState.SomeError
                    }
                }

                else -> BrandsState.SomeError
            }
        }
    }

    fun getAllModels(brandName: String) {
        viewModelScope.launch {
            _modelsState.value = ModelsState.Loading

            val res = CarModelsRepository.getAllModelsByBrandName(brandName)

            _modelsState.value = when (res) {
                is ApiResult.Success -> {
                    if (res.data != null) {
                        ModelsState.Data(res.data.map { it.modelName })
                    } else {
                        ModelsState.SomeError
                    }
                }

                else -> ModelsState.SomeError
            }
        }
    }

    fun resetUpdateCarState() {
        _updateCarState.value = UpdateCarState.Idle
    }

    fun startTrip(startDatetime: LocalDateTime, macAddress: String, carId: UInt) {
        viewModelScope.launch {
            _startTripState.value = StartTripState.Loading

            val body = StartTripRequest(startDatetime, macAddress, carId);

            val res = TripsRepository.startTrip(body)

            _startTripState.value = when (res) {
                ApiResult.NetworkError -> StartTripState.NetworkError
                is ApiResult.Success -> {
                    if (res.data == null) {
                        StartTripState.UnknownError
                    } else {
                        currentTrip = res.data
                        StartTripState.Data(res.data)
                    }
                }
                else -> StartTripState.UnknownError
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
    object SomeErrorToCreateAvatar: UpdatePersonState()
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
    object SomeErrorToCreatePhoto : UpdateCarState()
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

sealed class CheckBoxesDataState {
    object Idle : CheckBoxesDataState()
    object Loading : CheckBoxesDataState()
    object SomeError : CheckBoxesDataState()
    data class Data(
        val bodies: List<String>,
        val gearboxes: List<String>,
        val drives: List<String>,
        val fuelTypes: List<String>
    ): CheckBoxesDataState()
}

sealed class BrandsState {
    object Idle : BrandsState()
    object Loading : BrandsState()
    object SomeError : BrandsState()
    data class Data(
        val brands: List<String>
    ): BrandsState()
}

sealed class ModelsState {
    object Idle : ModelsState()
    object Loading : ModelsState()
    object SomeError : ModelsState()
    data class Data(
        val models: List<String>
    ): ModelsState()
}

sealed class StartTripState {
    object Idle : StartTripState()
    object Loading : StartTripState()
    object UnknownError : StartTripState()
    object NetworkError : StartTripState()
    object CarNotFound : StartTripState()
    data class Data(
        val trip: TripDto
    ): StartTripState()
}
