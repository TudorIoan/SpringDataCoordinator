package net.abaresults.progresspath.view.coordinator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.abaresults.progresspath.repo.OrgRepository
import net.abaresults.progresspath.repo.TherapistRepository
import net.abaresults.progresspath.repo.UserRepository
import net.abaresults.progresspath.view.objectives.ObjectivesState
import javax.inject.Inject

@HiltViewModel
class CoordinatorViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val orgRepo: OrgRepository
)  : ViewModel() {
    private val _state = MutableLiveData<CoordinatorState>()
        .apply { value = CoordinatorState.Idle }
    val state: LiveData<CoordinatorState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    fun takeAction(action: CoordinatorAction) {
        when (action) {
            is CoordinatorAction.Start -> handleStart()
        }
    }

    private fun update(newState: CoordinatorState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value = orgRepo.requireSelectedClinic().name

        update(CoordinatorState.Loading)
        viewModelScope.launch {
            val coordinatorResult = userRepo.fetchUserDetails(orgRepo.requireSelectedClinic().ownerUid)
            coordinatorResult.onSuccess {
                update(CoordinatorState.ContentLoaded(coordinatorResult.getOrNull()!!))
            }.onFailure { exception ->
                update(CoordinatorState.Error(exception.message ?: "Error"))
            }
        }
    }
}