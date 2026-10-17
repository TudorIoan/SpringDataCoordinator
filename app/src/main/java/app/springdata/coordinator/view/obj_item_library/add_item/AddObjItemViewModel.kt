package app.springdata.coordinator.view.obj_item_library.add_item

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import app.springdata.coordinator.model.ObjItem
import app.springdata.coordinator.model.ObjItemType
import app.springdata.coordinator.repo.ObjectiveRepository
import app.springdata.coordinator.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class AddObjItemViewModel @Inject constructor(
    private val objectiveRepo: ObjectiveRepository,
    private val orgRepo: OrgRepository
) : ViewModel() {
    private val _state = MutableLiveData<AddObjItemState>()
        .apply { value = AddObjItemState.Idle }
    val state: LiveData<AddObjItemState> = _state

    val objectiveName get() = orgRepo.requireSelectedObjective().name

    val level get() = orgRepo.requireSelectedObjective().level

    private var isSaving = false

    fun takeAction(action: AddObjItemAction) {
        when (action) {
            is AddObjItemAction.AddItemClicked -> handleAddItem(action.itemName, action.itemType)
        }
    }

    private fun update(newState: AddObjItemState) {
        _state.postValue(newState)
    }

    private fun handleAddItem(itemName: String, itemType: ObjItemType) {
        if (isSaving) return
        val name = itemName.trim()
        if (name.isEmpty()) {
            update(AddObjItemState.Error("Item name cannot be empty."))
            return
        }
        val objective = orgRepo.requireSelectedObjective()
        if (objective.itemsList.any { it.name.trim().equals(name, ignoreCase = true) }) {
            update(AddObjItemState.Error("Item already exists."))
            return
        }
        val newItem = ObjItem(name, name.lowercase(), itemType)
        val updatedObjective = objective.copy(
            itemsList = objective.itemsList.toMutableList().apply { add(newItem) }
        )
        isSaving = true
        update(AddObjItemState.Loading)
        viewModelScope.launch {
            try {
                objectiveRepo.updateObjective(updatedObjective).onSuccess {
                    orgRepo.setSelectedObjective(updatedObjective)
                    update(AddObjItemState.ItemAdded(name))
                }.onFailure { exception ->
                    update(AddObjItemState.Error(exception.message ?: "Error adding item"))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                update(AddObjItemState.Error(exception.message ?: "Error adding item"))
            } finally {
                isSaving = false
            }
        }
    }
}
