package app.springdata.coordinator.view.clinics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.model.Clinic
import app.springdata.coordinator.repo.ClinicRepository
import app.springdata.coordinator.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class ClinicsViewModel @Inject constructor(
    private val clinicsRepo: ClinicRepository,
    private val orgRepo: OrgRepository
)  : ViewModel() {
    private val _state = MutableLiveData<ClinicsState>()
        .apply { value = ClinicsState.Idle }
    val state: LiveData<ClinicsState> = _state

    val clinicsList = mutableListOf<Clinic>()

    fun takeAction(action: ClinicsAction) {
        when (action) {
            is ClinicsAction.Start -> handleStart()
            is ClinicsAction.ClinicClicked -> handleClinicClicked(action.clinic)
            is ClinicsAction.RemoveClinic -> handleRemoveClinic(action.clinic)
            is ClinicsAction.UpdateClinicName -> handleUpdateClinicName(action.clinic)
        }
    }

    private fun update(newState: ClinicsState) {
        _state.value = newState
    }

    private fun handleStart() {
        update(ClinicsState.Loading)
        viewModelScope.launch {
            val result = clinicsRepo.fetchAllClinicsCreatedByCurrentUser()

            result.onSuccess {
                clinicsList.clear()
                clinicsList.addAll(it)
                update(ClinicsState.ContentLoaded(clinicsList))
            }.onFailure { exception ->
                update(ClinicsState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleClinicClicked(clinic: Clinic) {
        orgRepo.setSelectedClinic(clinic)
        update(ClinicsState.GoToKids)
        update(ClinicsState.Idle)
    }

    private fun handleRemoveClinic(clinic: Clinic) {
        update(ClinicsState.Loading)
        viewModelScope.launch {
            val result = clinicsRepo.removeClinic(clinic)
            result.onSuccess {
                clinicsList.removeIf { it.id == clinic.id }
                update(ClinicsState.ContentLoaded(clinicsList))
            }.onFailure { exception ->
                update(ClinicsState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleUpdateClinicName(clinic: Clinic) {
        update(ClinicsState.Loading)
        viewModelScope.launch {
            val result = clinicsRepo.updateClinic(clinic)
            result.onSuccess {
                val index = clinicsList.indexOfFirst { it.id == clinic.id }
                if (index != -1) {
                    clinicsList[index] = clinic
                }
                update(ClinicsState.ContentLoaded(clinicsList))
            }.onFailure { exception ->
                update(ClinicsState.Error(exception.message ?: "Error"))
            }
        }
    }

}
