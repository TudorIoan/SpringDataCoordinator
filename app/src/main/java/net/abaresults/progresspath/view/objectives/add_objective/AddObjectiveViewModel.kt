package net.abaresults.progresspath.view.objectives.add_objective

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.Objective
import net.abaresults.progresspath.model.ObjectiveType
import net.abaresults.progresspath.repo.KidObjectiveRepository
import net.abaresults.progresspath.repo.ObjectiveRepository
import net.abaresults.progresspath.repo.OrgRepository
import net.abaresults.progresspath.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class AddObjectiveViewModel @Inject constructor(
    private val objectiveRepo: ObjectiveRepository,
    private val userRepo: UserRepository,
    private val kidObjectiveRepo: KidObjectiveRepository,
    private val orgRepo: OrgRepository
) : ViewModel() {
    private val _state = MutableLiveData<AddObjectiveState>()
        .apply { value = AddObjectiveState.Idle }
    val state: LiveData<AddObjectiveState> = _state

    private val _availableObjectives = MutableLiveData<List<Objective>>()
    val availableObjectives: LiveData<List<Objective>> = _availableObjectives

    private val _selectedLevel = MutableLiveData<ObjLevel>()
    val selectedLevel: LiveData<ObjLevel> = _selectedLevel

    private val _availableObjectiveTypes = MutableLiveData<List<ObjectiveType>>()
    val availableObjectiveTypes: LiveData<List<ObjectiveType>> = _availableObjectiveTypes

    fun takeAction(action: AddObjectiveAction) {
        when (action) {
            is AddObjectiveAction.Start -> handleStart()
            is AddObjectiveAction.ObjLevelChanged -> handleObjLevelChanged(action.level)
            is AddObjectiveAction.ObjectiveTypeChanged -> handleObjectiveTypeChanged(action.objectiveType)
            is AddObjectiveAction.ObjectiveSelected -> handleObjectiveSelected(action.objective)
        }
    }

    private fun update(newState: AddObjectiveState) {
        _state.postValue(newState)
    }

    private fun handleStart() {
        // Start with saved or BEGINNER level
        handleObjLevelChanged(orgRepo.getSelectedLevel() ?: ObjLevel.BEGINNER)
    }

    private fun handleObjLevelChanged(level: ObjLevel) {
        orgRepo.setSelectedLevel(level)

        _selectedLevel.postValue(level)

        // Get objective types for the selected level
        val objectiveTypesForLevel = ObjectiveType.getObjectiveTypesByLevel(level)
        _availableObjectiveTypes.postValue(objectiveTypesForLevel)

        // Load objectives for the first type of this level
        if (objectiveTypesForLevel.isNotEmpty()) {
            handleObjectiveTypeChanged(objectiveTypesForLevel.first())
        } else {
            _availableObjectives.postValue(emptyList())
            update(AddObjectiveState.ContentLoaded)
        }
    }

    private fun handleObjectiveTypeChanged(objectiveType: ObjectiveType) {
        viewModelScope.launch {
            try {
                update(AddObjectiveState.Loading)
                
                val kidId = orgRepo.requireSelectedKid().id
                
                // Get all objectives of the selected type
                val allObjectivesResult = objectiveRepo.fetchObjectivesByType(objectiveType, userRepo.requireUserDetails().ownerUid)
                
                // Get kid objectives to filter out already assigned ones
                val kidObjectivesResult = kidObjectiveRepo.getKidObjectives(kidId)
                
                val finalResult = allObjectivesResult.mapCatching { allObjectives ->
                    kidObjectivesResult.mapCatching { kidObjectives ->
                        val assignedObjectiveIds = kidObjectives.map { it.objectiveId }.toSet()
                        val availableObjectives = allObjectives.filter { !assignedObjectiveIds.contains(it.id) }
                        availableObjectives
                    }.getOrThrow()
                }.getOrThrow()
                
                _availableObjectives.postValue(finalResult)
                update(AddObjectiveState.ContentLoaded)
            } catch (exception: Exception) {
                update(AddObjectiveState.Error(exception.message ?: "Error loading objectives"))
            }
        }
    }

    private fun handleObjectiveSelected(objective: Objective) {
        viewModelScope.launch {
            try {
                update(AddObjectiveState.Loading)
                
                val kidId = orgRepo.requireSelectedKid().id
                val result = kidObjectiveRepo.addKidObjective(kidId, objective, false)
                
                result.fold(
                    onSuccess = {
                        // Refresh the available objectives list by removing the added objective
                        val currentObjectives = _availableObjectives.value ?: emptyList()
                        val updatedObjectives = currentObjectives.filter { it.id != objective.id }
                        _availableObjectives.postValue(updatedObjectives)

                        update(AddObjectiveState.ObjectiveAdded(objective.name))
                    },
                    onFailure = { exception ->
                        update(AddObjectiveState.Error(exception.message ?: "Error adding objective"))
                    }
                )
            } catch (exception: Exception) {
                update(AddObjectiveState.Error(exception.message ?: "Error adding objective"))
            }
        }
    }
}