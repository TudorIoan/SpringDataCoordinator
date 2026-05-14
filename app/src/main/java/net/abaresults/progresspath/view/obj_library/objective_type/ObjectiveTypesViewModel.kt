package net.abaresults.progresspath.view.obj_library.objective_type

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.repo.ObjectiveTypesRepository
import javax.inject.Inject

@HiltViewModel
class ObjectiveTypesViewModel @Inject constructor(
    private val objectiveTypesRepository: ObjectiveTypesRepository
) : ViewModel() {

    private val _state = MutableLiveData<ObjectiveTypesState>(ObjectiveTypesState.Idle)
    val state: LiveData<ObjectiveTypesState> = _state

    fun takeAction(action: ObjectiveTypesAction) {
        when (action) {
            is ObjectiveTypesAction.Start -> handleStart()
            is ObjectiveTypesAction.LoadObjectiveTypes -> handleLoadObjectiveTypes(action.level)
            is ObjectiveTypesAction.AddObjectiveType -> handleAddObjectiveType(action.name, action.level)
            is ObjectiveTypesAction.DeleteObjectiveType -> handleDeleteObjectiveType(action.id)
        }
    }

    private fun handleStart() {
        // Load beginner level by default
        handleLoadObjectiveTypes(ObjLevel.BEGINNER)
    }

    private fun handleLoadObjectiveTypes(level: ObjLevel) {
        viewModelScope.launch {
            _state.value = ObjectiveTypesState.Loading

            val result = objectiveTypesRepository.getObjectiveTypesByLevel(level)

            result.fold(
                onSuccess = { objectiveTypes ->
                    _state.value = ObjectiveTypesState.ContentLoaded(objectiveTypes)
                },
                onFailure = { error ->
                    _state.value = ObjectiveTypesState.Error(error.message ?: "Failed to load objective types")
                }
            )
        }
    }

    private fun handleAddObjectiveType(name: String, level: ObjLevel) {
        viewModelScope.launch {
            val result = objectiveTypesRepository.addObjectiveType(name, level)

            result.fold(
                onSuccess = {
                    _state.value = ObjectiveTypesState.ObjectiveTypeAdded
                },
                onFailure = { error ->
                    _state.value = ObjectiveTypesState.Error(error.message ?: "Failed to add objective type")
                }
            )
        }
    }

    private fun handleDeleteObjectiveType(id: Long) {
        viewModelScope.launch {
            val result = objectiveTypesRepository.deleteObjectiveType(id)

            result.fold(
                onSuccess = {
                    _state.value = ObjectiveTypesState.ObjectiveTypeDeleted
                },
                onFailure = { error ->
                    _state.value = ObjectiveTypesState.Error(error.message ?: "Failed to delete objective type")
                }
            )
        }
    }
}