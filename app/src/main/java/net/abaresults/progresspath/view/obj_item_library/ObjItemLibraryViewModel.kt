package net.abaresults.progresspath.view.obj_item_library

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.ObjItem
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.repo.ObjectiveRepository
import net.abaresults.progresspath.repo.OrgRepository
import net.abaresults.progresspath.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class ObjItemLibraryViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val objectiveRepo: ObjectiveRepository,
    private val orgRepo: OrgRepository
) : ViewModel() {
    private val _state = MutableLiveData<ObjItemLibraryState>()
        .apply { value = ObjItemLibraryState.Idle }
    val state: LiveData<ObjItemLibraryState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _userType = MutableLiveData<UserType>()
    val userType: LiveData<UserType> = _userType

    var items: List<ObjItem> = listOf()


    fun takeAction(action: ObjItemLibraryAction) {
        Log.d("ObjItemLibraryViewModel", "Action received: ${action::class.simpleName}")
        when (action) {
            is ObjItemLibraryAction.Start -> handleStart()
            is ObjItemLibraryAction.UpdateItem -> handleUpdateItem(action.item)
            is ObjItemLibraryAction.RemoveItem -> handleRemoveItem(action.item)
        }
    }

    private fun update(newState: ObjItemLibraryState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value = "Objective: ${orgRepo.requireSelectedObjective().name}"
        _userType.value = UserType.fromString(userRepo.requireUserDetails().userType)
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
}