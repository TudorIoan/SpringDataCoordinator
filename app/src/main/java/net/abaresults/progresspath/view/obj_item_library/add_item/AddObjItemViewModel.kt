package net.abaresults.progresspath.view.obj_item_library.add_item

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.ObjItem
import net.abaresults.progresspath.model.ObjItemType
import net.abaresults.progresspath.repo.ObjectiveRepository
import net.abaresults.progresspath.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class AddObjItemViewModel @Inject constructor(
    private val objectiveRepo: ObjectiveRepository,
    private val orgRepo: OrgRepository
) : ViewModel() {
    private val _state = MutableLiveData<AddObjItemState>()
        .apply { value = AddObjItemState.Idle }
    val state: LiveData<AddObjItemState> = _state

    fun takeAction(action: AddObjItemAction) {
        when (action) {
            is AddObjItemAction.AddItemClicked -> handleAddItem(action.itemName, action.itemType)
        }
    }

    private fun update(newState: AddObjItemState) {
        _state.postValue(newState)
    }

    private fun handleAddItem(itemName: String, itemType: ObjItemType) {
        update(AddObjItemState.Loading)
            if (orgRepo.requireSelectedObjective().itemsList.any { it.normalizedName == itemName.trim().lowercase() }) {
                update(AddObjItemState.Error("Item already exists"))
            } else {
                val newItem = ObjItem(itemName.trim(), itemName.trim().lowercase(), itemType)
                val updatedItemsList = orgRepo.requireSelectedObjective().itemsList.toMutableList().apply { add(newItem) }
                val updatedObjective = orgRepo.requireSelectedObjective().copy(itemsList = updatedItemsList)

                viewModelScope.launch {
                    orgRepo.setSelectedObjective(updatedObjective)
                    val result = objectiveRepo.updateObjective(updatedObjective)
                    result.onSuccess {
                        update(AddObjItemState.ItemAdded(itemName.trim()))
                    }.onFailure { exception ->
                        update(AddObjItemState.Error(exception.message ?: "Error adding item"))
                    }
                }
            }
    }
}
