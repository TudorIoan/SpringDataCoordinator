package net.abaresults.progresspath.view.obj_library

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.Objective
import net.abaresults.progresspath.model.ObjectiveType
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.repo.ObjectiveRepository
import net.abaresults.progresspath.repo.OrgRepository
import net.abaresults.progresspath.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class ObjLibraryViewModel @Inject constructor(
    private val objectiveRepo: ObjectiveRepository,
    private val orgRepo: OrgRepository,
    private val userRepo: UserRepository
)  : ViewModel() {
    private val _state = MutableLiveData<ObjLibraryState>()
        .apply { value = ObjLibraryState.Idle }
    val state: LiveData<ObjLibraryState> = _state

    private val _userType = MutableLiveData<UserType>()
    val userType: LiveData<UserType> = _userType

    val objectives = mutableListOf<Objective>()
    val objLibraryListItems = mutableListOf<ObjLibraryListItem>()
    private var selectedLevel = orgRepo.getSelectedLevel() ?: ObjLevel.BEGINNER

    fun takeAction(action: ObjLibraryAction) {
        when (action) {
            is ObjLibraryAction.Start -> handleStart()
            is ObjLibraryAction.ObjectiveClicked -> handleObjectiveClicked(action.objectiveListItem)
            is ObjLibraryAction.AddObjectiveClicked -> handleAddObjectiveClicked()
            is ObjLibraryAction.RemoveObjective -> handleRemoveObjective(action.objective)
            is ObjLibraryAction.UpdateObjective -> handleUpdateObjective(action.objective)
            is ObjLibraryAction.LevelTabSelected -> handleLevelTabSelected(action.level)
        }
    }

    private fun update(newState: ObjLibraryState) {
        _state.value = newState
    }

    private fun handleStart() {
        _userType.value = UserType.fromString(userRepo.requireUserDetails().userType)

        update(ObjLibraryState.Loading)

        objectives.clear()
        objLibraryListItems.clear()

        viewModelScope.launch {
            val userType = UserType.fromString(userRepo.requireUserDetails().userType)

            Log.d("ObjLibraryViewModel", "User type: $userType")

            // Get all objectives from the library
            val objectivesResult = objectiveRepo.fetchAllObjectives(userRepo.requireUserDetails().ownerUid)

            objectivesResult.onSuccess { objectivesList ->
                Log.d("ObjLibraryViewModel", "Found ${objectivesList.size} objectives in library")
                objectives.addAll(objectivesList)

                // Filter and group objectives by level and type
                refreshObjectivesList()

                update(ObjLibraryState.ContentLoaded(objLibraryListItems, selectedLevel))
            }.onFailure { exception ->
                Log.e("ObjLibraryViewModel", "Error fetching objectives", exception)
                update(ObjLibraryState.Error(exception.message ?: "Error fetching objectives"))
            }
        }
    }

    private fun handleObjectiveClicked(objLibraryListItem: ObjLibraryListItem) {
        when (objLibraryListItem) {
            is ObjLibraryListItem.ObjectiveTypeItem -> {
                var itemIndex = objLibraryListItems.indexOfFirst { it == objLibraryListItem }
                objLibraryListItems[itemIndex] = objLibraryListItem.copy(expanded = !objLibraryListItem.expanded)
                if ((objLibraryListItems[itemIndex] as ObjLibraryListItem.ObjectiveTypeItem).expanded) {
                    objectives.filter { it.type == objLibraryListItem.objectiveType }.forEach { objective ->
                        itemIndex ++
                        objLibraryListItems.add(itemIndex, ObjLibraryListItem.ObjectiveItem(objective))
                    }
                } else {
                    objLibraryListItems.removeAll { it is ObjLibraryListItem.ObjectiveItem && it.objective.type == objLibraryListItem.objectiveType }
                }
                update(ObjLibraryState.ContentLoaded(objLibraryListItems, selectedLevel))
            }
            is ObjLibraryListItem.ObjectiveItem -> {
                // Navigate to objective items library
                orgRepo.setSelectedObjective(objLibraryListItem.objective)
                update(ObjLibraryState.GoToItemLibrary(objLibraryListItem.objective))
                update(ObjLibraryState.Idle)
            }
        }
    }

    private fun handleAddObjectiveClicked() {
        orgRepo.setSelectedLevel(selectedLevel)
        update(ObjLibraryState.GoToAddObjective)
    }

    private fun handleRemoveObjective(objective: Objective) {
        update(ObjLibraryState.Loading)
        viewModelScope.launch {
            val result = objectiveRepo.removeObjective(objective.id)
            result.onSuccess {
                // Remove from local lists
                objectives.removeIf { it.id == objective.id }
                refreshObjectivesList()
                update(ObjLibraryState.ContentLoaded(objLibraryListItems, selectedLevel))
            }.onFailure { exception ->
                update(ObjLibraryState.Error(exception.message ?: "Error removing objective"))
            }
        }
    }

    private fun handleUpdateObjective(objective: Objective) {
        update(ObjLibraryState.Loading)
        viewModelScope.launch {
            val result = objectiveRepo.updateObjective(objective)
            result.onSuccess {
                // Update local lists
                val objIndex = objectives.indexOfFirst { it.id == objective.id }
                if (objIndex != -1) {
                    objectives[objIndex] = objective
                }
                refreshObjectivesList()
                update(ObjLibraryState.ContentLoaded(objLibraryListItems, selectedLevel))
            }.onFailure { exception ->
                update(ObjLibraryState.Error(exception.message ?: "Error updating objective"))
            }
        }
    }

    private fun handleLevelTabSelected(level: ObjLevel) {
        selectedLevel = level
        refreshObjectivesList()
        update(ObjLibraryState.ContentLoaded(objLibraryListItems, selectedLevel))
    }

    private fun refreshObjectivesList() {
        objLibraryListItems.clear()

        // Filter objectives by selected level
        val filteredObjectives = objectives.filter { it.level == selectedLevel }

        val objectiveTypes = filteredObjectives.map { it.type }.toSet()
        objectiveTypes.forEach { objectiveType ->
            objLibraryListItems.add(ObjLibraryListItem.ObjectiveTypeItem(objectiveType, expanded = false))
        }
    }
}