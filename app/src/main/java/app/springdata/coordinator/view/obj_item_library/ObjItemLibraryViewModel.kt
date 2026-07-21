package app.springdata.coordinator.view.obj_item_library

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.model.ObjItem
import app.springdata.coordinator.repo.ClinicRepository
import app.springdata.coordinator.repo.KidObjectiveRepository
import app.springdata.coordinator.repo.KidRepository
import app.springdata.coordinator.repo.ObjectiveRepository
import app.springdata.coordinator.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class ObjItemLibraryViewModel @Inject constructor(
    private val objectiveRepo: ObjectiveRepository,
    private val kidObjectiveRepo: KidObjectiveRepository,
    private val kidRepo: KidRepository,
    private val clinicRepo: ClinicRepository,
    private val orgRepo: OrgRepository
) : ViewModel() {
    private val _state = MutableLiveData<ObjItemLibraryState>()
        .apply { value = ObjItemLibraryState.Idle }
    val state: LiveData<ObjItemLibraryState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    var items: List<ObjItem> = listOf()


    fun takeAction(action: ObjItemLibraryAction) {
        Log.d("ObjItemLibraryViewModel", "Action received: ${action::class.simpleName}")
        when (action) {
            is ObjItemLibraryAction.Start -> handleStart()
            is ObjItemLibraryAction.UpdateItem -> handleUpdateItem(action.item)
            is ObjItemLibraryAction.RemoveItem -> handleRemoveItem(action.item)
            is ObjItemLibraryAction.ShowObjectiveKids -> handleShowObjectiveKids()
        }
    }

    private fun update(newState: ObjItemLibraryState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value = "Objective: ${orgRepo.requireSelectedObjective().name}"
        updateItems()
    }

    private fun updateItems() {
        items = orgRepo.requireSelectedObjective().itemsList
        update(ObjItemLibraryState.ContentLoaded(items))
    }

    private fun handleUpdateItem(updatedObjItem: ObjItem) {
        update(ObjItemLibraryState.Loading)

        // Check if new item name is unique in the items list
        if (items.any { it.normalizedName == updatedObjItem.name.lowercase() && it.normalizedName != updatedObjItem.normalizedName }) {
            update(ObjItemLibraryState.Error("Item name already exists"))
            return
        }

        val newObjItem = updatedObjItem.copy(
            name = updatedObjItem.name,
            normalizedName = updatedObjItem.name.lowercase()
        )
        val newItemsList = orgRepo.requireSelectedObjective().itemsList.map {
            if (it.normalizedName == updatedObjItem.normalizedName) {
                newObjItem
            } else it
        }
        val updatedObjective =
            orgRepo.requireSelectedObjective().copy(itemsList = newItemsList.toMutableList())

        viewModelScope.launch {
            val result = objectiveRepo.updateObjective(updatedObjective)
            result.onSuccess {
                orgRepo.setSelectedObjective(updatedObjective)
                updateItems()
            }.onFailure { exception ->
                update(ObjItemLibraryState.Error(exception.message ?: "Error updating item"))
            }
        }
    }

    private fun handleRemoveItem(itemToRemove: ObjItem) {
        update(ObjItemLibraryState.Loading)
        val newItemsList = orgRepo.requireSelectedObjective().itemsList.filter {
            it.normalizedName != itemToRemove.normalizedName
        }
        val updatedObjective =
            orgRepo.requireSelectedObjective().copy(itemsList = newItemsList.toMutableList())

        viewModelScope.launch {
            val result = objectiveRepo.updateObjective(updatedObjective)
            result.onSuccess {
                orgRepo.setSelectedObjective(updatedObjective)
                updateItems()
            }.onFailure { exception ->
                update(ObjItemLibraryState.Error(exception.message ?: "Error removing item"))
            }
        }
    }

    private fun handleShowObjectiveKids() {
        update(ObjItemLibraryState.Loading)

        viewModelScope.launch {
            val objective = orgRepo.requireSelectedObjective()
            val kidObjectivesResult = kidObjectiveRepo.getKidObjectivesForObjective(objective.id)

            kidObjectivesResult.onSuccess { kidObjectives ->
                val kidIds = kidObjectives.map { it.kidId }.filter { it.isNotBlank() }.distinct()
                val kidItems = kidIds.mapNotNull { kidId ->
                    val kid = kidRepo.getKidById(kidId).getOrNull() ?: return@mapNotNull null
                    val clinicName = clinicRepo.getClinicById(kid.clinicId).getOrNull()?.name ?: "Unknown clinic"
                    ObjectiveKidListItem(kid.name, clinicName)
                }.sortedWith(compareBy<ObjectiveKidListItem> { it.clinicName.lowercase() }.thenBy { it.kidName.lowercase() })

                update(ObjItemLibraryState.ObjectiveKidsLoaded(kidItems))
            }.onFailure { exception ->
                update(ObjItemLibraryState.Error(exception.message ?: "Error loading kids"))
            }
        }
    }
}
