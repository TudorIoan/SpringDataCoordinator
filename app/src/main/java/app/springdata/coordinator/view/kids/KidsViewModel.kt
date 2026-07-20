package app.springdata.coordinator.view.kids

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.model.Kid
import app.springdata.coordinator.repo.KidRepository
import app.springdata.coordinator.repo.OrgRepository
import app.springdata.coordinator.repo.TherapistRepository
import app.springdata.coordinator.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class KidsViewModel @Inject constructor(
    private val kidRepo: KidRepository,
    private val orgRepo: OrgRepository,
    private val userRepo: UserRepository,
    private val therapistRepo: TherapistRepository
) : ViewModel() {
    private val _state = MutableLiveData<KidsState>()
        .apply { value = KidsState.Idle }
    val state: LiveData<KidsState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    val kidsList = mutableListOf<Kid>()

    fun takeAction(action: KidsAction) {
        when (action) {
            is KidsAction.Start -> handleStart()
            is KidsAction.KidClicked -> handleKidClicked(action.kid)
            is KidsAction.RemoveKid -> handleRemoveKid(action.kid)
            is KidsAction.UpdateKidName -> handleUpdateKidName(action.kid)
        }
    }

    private fun update(newState: KidsState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value = orgRepo.requireSelectedClinic().name

        orgRepo.setSelectedClinic(orgRepo.requireSelectedClinic())

        update(KidsState.Loading)
        viewModelScope.launch {
            therapistRepo.fetchTherapistsForClinic(orgRepo.requireSelectedClinic().id)
                .onSuccess { userRepo.therapistUsers = it }

            kidRepo.fetchAllKidsForClinic(orgRepo.requireSelectedClinic().id)
                .onSuccess {
                    kidsList.clear()
                    kidsList.addAll(it)
                    update(KidsState.ContentLoaded(kidsList))
                }.onFailure { exception ->
                    update(KidsState.Error(exception.message ?: "Error"))
                }
        }
    }

    private fun handleKidClicked(kid: Kid) {
        orgRepo.setSelectedKid(kid)
        update(KidsState.GoToObjectives)
        update(KidsState.Idle)
    }

    private fun handleRemoveKid(kid: Kid) {
        update(KidsState.Loading)
        viewModelScope.launch {
            kidRepo.removeKid(kid)
                .onSuccess {
                    kidsList.removeIf { it.id == kid.id }
                    update(KidsState.ContentLoaded(kidsList))
                }.onFailure { exception ->
                    update(KidsState.Error(exception.message ?: "Error"))
                }
        }
    }

    private fun handleUpdateKidName(kid: Kid) {
        update(KidsState.Loading)
        viewModelScope.launch {
            kidRepo.updateKid(kid)
                .onSuccess {
                    val index = kidsList.indexOfFirst { it.id == kid.id }
                    if (index != -1) {
                        kidsList[index] = kid
                    }
                    update(KidsState.ContentLoaded(kidsList))
                }.onFailure { exception ->
                    update(KidsState.Error(exception.message ?: "Error"))
                }
        }
    }
}
