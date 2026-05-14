package net.abaresults.progresspath.view.clinics.add_clinic

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.repo.ClinicRepository
import javax.inject.Inject

@HiltViewModel
class AddClinicViewModel @Inject constructor(
    private val clinicRepo: ClinicRepository
)  : ViewModel() {
    private val _state = MutableLiveData<AddClinicState>()
        .apply { value = AddClinicState.Idle }
    val state: LiveData<AddClinicState> = _state

    fun takeAction(action: AddClinicAction) {
        when (action) {
            is AddClinicAction.AddClinicClicked -> handleAddClinic(action.clinicName)
        }
    }

    private fun update(newState: AddClinicState) {
        _state.postValue(newState)
    }

    fun handleAddClinic(clinicName: String) {
        update(AddClinicState.Loading)
        viewModelScope.launch {
            val result = clinicRepo.addClinic(clinicName)
            result.onSuccess { update(AddClinicState.ClinicAdded)
            }.onFailure { exception ->
                update(AddClinicState.Error(exception.message ?: "Error"))
            }
        }
    }

}