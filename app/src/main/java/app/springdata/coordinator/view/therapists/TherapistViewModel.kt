package app.springdata.coordinator.view.therapists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import app.springdata.coordinator.repo.KidRepository
import app.springdata.coordinator.repo.OrgRepository
import app.springdata.coordinator.repo.TherapistRepository
import javax.inject.Inject

@HiltViewModel
class TherapistViewModel @Inject constructor(
    private val therapistRepo: TherapistRepository,
    private val kidsRepo: KidRepository,
    private val orgRepo: OrgRepository
)  : ViewModel() {
    private val _state = MutableLiveData<TherapistState>()
        .apply { value = TherapistState.Idle }
    val state: LiveData<TherapistState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    val therapistsList = mutableListOf<TherapistVM>()

    fun takeAction(action: TherapistAction) {
        when (action) {
            is TherapistAction.Start -> handleStart()
            is TherapistAction.TherapistClicked -> handleTherapistClicked(action.therapist)
            is TherapistAction.RemoveTherapist -> handleRemoveTherapist(action.therapist)
        }
    }

    private fun update(newState: TherapistState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value = orgRepo.requireSelectedClinic().name
        val isClinic = orgRepo.getSelectedKid() == null

        update(TherapistState.Loading)
        viewModelScope.launch {
            val therapistUsersDeferred = if (orgRepo.getSelectedKid() != null) {
                async { therapistRepo.fetchTherapistsForKid(orgRepo.requireSelectedKid()) }
            } else {
                async { therapistRepo.fetchTherapistsForClinic(orgRepo.requireSelectedClinic().id) }
            }

            val invitedTherapistsDeferred = if (orgRepo.getSelectedKid() != null) {
                async { therapistRepo.fetchInvitedTherapistsForKid(orgRepo.requireSelectedKid()) }
            } else {
                async { therapistRepo.fetchInvitedTherapistsForClinic(orgRepo.requireSelectedClinic().id) }
            }

            val therapistUsersResult = therapistUsersDeferred.await()
            val invitedTherapistsResult = invitedTherapistsDeferred.await()

            if (therapistUsersResult.isSuccess && invitedTherapistsResult.isSuccess) {
                val therapistProfiles = therapistUsersResult.getOrThrow()
                val invitedTherapists = invitedTherapistsResult.getOrThrow()

                val combinedList = mutableListOf<TherapistVM>()

                // Map UserProfiles
                therapistProfiles.forEach { profile ->
                    combinedList.add(
                        TherapistVM(
                            name = profile.name,
                            email = profile.email,
                            isOnlyInvited = false
                        )
                    )
                }

                invitedTherapists.forEach { invite ->
                        combinedList.add(
                            TherapistVM(
                                name = "Invited: Did not create account yet",
                                email = invite.email,
                                isOnlyInvited = true
                            )
                        )
                }

                therapistsList.clear()
                therapistsList.addAll(combinedList)
                update(TherapistState.ContentLoaded(combinedList, isClinic))

            } else {
                val errorMessages = mutableListOf<String>()
                therapistUsersResult.exceptionOrNull()?.message?.let { errorMessages.add("Failed to load therapists: $it") }
                invitedTherapistsResult.exceptionOrNull()?.message?.let { errorMessages.add("Failed to load invites: $it") }
                val combinedErrorMessage = if (errorMessages.isNotEmpty()) errorMessages.joinToString("\n") else "Unknown error occurred"
                update(TherapistState.Error(combinedErrorMessage))
            }
        }
    }

    private fun handleTherapistClicked(therapist: TherapistVM) {

    }

    private fun handleRemoveTherapist(therapist: TherapistVM) {
        update(TherapistState.Loading)
        viewModelScope.launch {
            val currentKid = orgRepo.getSelectedKid()
            val result = if (currentKid != null) {
                // Remove from specific kid
                therapistRepo.removeTherapistFromKid(therapist.email, currentKid)
            } else {
                // Remove from entire clinic
                therapistRepo.removeTherapistFromClinic(therapist.name, orgRepo.requireSelectedClinic().id)
            }

            result.onSuccess { _ ->
                // If we removed from a specific kid, refresh the kid data in orgRepo
                if (currentKid != null) {
                    kidsRepo.getKidById(currentKid.id).onSuccess { updatedKid ->
                        updatedKid?.let { orgRepo.setSelectedKid(it) }
                    }
                }

                // Remove from local list
                therapistsList.removeIf { it.name == therapist.name }
                val isClinic = orgRepo.getSelectedKid() == null
                update(TherapistState.ContentLoaded(therapistsList.toList(), isClinic))
            }.onFailure { exception ->
                update(TherapistState.Error(exception.message ?: "Error removing therapist"))
            }
        }
    }
}