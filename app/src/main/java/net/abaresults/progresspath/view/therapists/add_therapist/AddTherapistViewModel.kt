package net.abaresults.progresspath.view.therapists.add_therapist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.repo.KidRepository
import net.abaresults.progresspath.repo.OrgRepository
import net.abaresults.progresspath.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class AddTherapistViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val kidRepo: KidRepository,
    private val orgRepo: OrgRepository
)  : ViewModel() {
    private val _state = MutableLiveData<AddTherapistState>()
        .apply { value = AddTherapistState.Idle }
    val state: LiveData<AddTherapistState> = _state

    fun takeAction(action: AddTherapistAction) {
        when (action) {
            is AddTherapistAction.AddTherapistClicked -> handleAddTherapist(action.therapistEmail)
        }
    }

    private fun update(newState: AddTherapistState) {
        _state.postValue(newState)
    }

    fun handleAddTherapist(therapistEmail: String) {
        update(AddTherapistState.Loading)
        viewModelScope.launch {
            val result = kidRepo.addTherapistToKidOrInvite(therapistEmail, orgRepo.requireSelectedKid().id, userRepo.requireUserDetails().name)
            result.onSuccess { update(AddTherapistState.TherapistAdded)
            }.onFailure { exception ->
                update(AddTherapistState.Error(exception.message ?: "Error"))
            }
        }
    }

}