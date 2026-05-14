package net.abaresults.progresspath.view.obj_library.add_objective

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.ObjectiveType
import net.abaresults.progresspath.repo.ObjectiveRepository
import net.abaresults.progresspath.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class AddObjLibraryViewModel @Inject constructor(
    private val objectiveRepo: ObjectiveRepository,
    private val orgRepo: OrgRepository
) : ViewModel() {
    private val _state = MutableLiveData<AddObjLibraryState>()
        .apply { value = AddObjLibraryState.Idle }
    val state: LiveData<AddObjLibraryState> = _state

    fun takeAction(action: AddObjLibraryAction) {
        when (action) {
            is AddObjLibraryAction.Start -> handleStart()
            is AddObjLibraryAction.CreateObjectiveClicked -> handleCreateObjective(action.objectiveType, action.objectiveName, action.level)
        }
    }

    private fun update(newState: AddObjLibraryState) {
        _state.postValue(newState)
    }

    private fun handleStart() {
        update(AddObjLibraryState.ContentLoaded(orgRepo.requireSelectedLevel()))
    }

    private fun handleCreateObjective(objectiveType: ObjectiveType, objectiveName: String, level: ObjLevel) {
        update(AddObjLibraryState.Loading)
        viewModelScope.launch {
            try {
                val result = objectiveRepo.addObjective(objectiveType, objectiveName, mutableListOf(), level)

                result.fold(
                    onSuccess = { update(AddObjLibraryState.ObjectiveCreated(objectiveName)) },
                    onFailure = { exception ->
                        update(AddObjLibraryState.Error(exception.message ?: "Unknown error creating objective"))
                    }
                )
            } catch (exception: Exception) {
                update(AddObjLibraryState.Error(exception.message ?: "Unknown error creating objective"))
            }
        }
    }
}