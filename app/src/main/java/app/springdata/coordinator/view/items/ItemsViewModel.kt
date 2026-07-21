package app.springdata.coordinator.view.items

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.model.FrequencyItem
import app.springdata.coordinator.model.ProgressItem
import app.springdata.coordinator.model.KidObjectiveItem
import app.springdata.coordinator.model.YesNoItem
import app.springdata.coordinator.repo.KidObjectiveRepository
import app.springdata.coordinator.repo.ObjectiveRepository
import app.springdata.coordinator.repo.OrgRepository
import app.springdata.coordinator.repo.UserRepository
import app.springdata.coordinator.view.items.ItemState.Idle
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val orgRepo: OrgRepository,
    private val userRepo: UserRepository,
    private val objectiveRepo: ObjectiveRepository,
    private val kidObjectiveRepo: KidObjectiveRepository
) : ViewModel() {
    private val _state = MutableLiveData<ItemState>()
        .apply { value = ItemState.Idle }
    val state: LiveData<ItemState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    var items: List<KidObjectiveItem> = listOf()

    fun takeAction(action: ItemAction) {
        Log.d("ItemsViewModel", "Action received: ${action::class.simpleName}")
        when (action) {
            is ItemAction.Start -> handleStart()
            is ItemAction.ItemToggleClicked -> {
                Log.d("ItemsViewModel", "Calling handleToggleClicked")
                handleToggleClicked(action.item)
            }

            is ItemAction.ItemYesClicked -> handleYesClicked(action.item)
            is ItemAction.ItemNoClicked -> handleNoClicked(action.item)
            is ItemAction.RemoveItem -> handleRemoveItem(action.item)
            is ItemAction.FrequencySet -> handleFrequencySet(action.item, action.frequency, action.interval)
            is ItemAction.ProgressSet -> handleProgressSet(action.item, action.progress)
            is ItemAction.SetMastered -> handleSetMastered(action.item)
            is ItemAction.CheckmarkClicked -> handleCheckmarkClicked(action.item)
        }

    }

    private fun update(newState: ItemState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value =
            "${orgRepo.requireSelectedClinic().name} > ${orgRepo.requireSelectedKid().name} > ${orgRepo.requireSelectedObjective().name}"

        updateItems()
    }

    private fun updateItems() {

        val filteredItems = orgRepo.requireSelectedKidObjective().itemsList


        items = filteredItems.sortedWith(compareBy {
            when {
                it.active && !it.mastered -> 0
                !it.active && !it.mastered -> 1
                else -> 2 // mastered
            }
        })

        update(ItemState.ContentLoaded(items))
    }

    private fun handleToggleClicked(item: KidObjectiveItem) {
        update(ItemState.Loading)
        viewModelScope.launch {
            val result = kidObjectiveRepo.updateKidObjective(orgRepo.requireSelectedKidObjective())
            result.onSuccess {
                update(Idle)
            }.onFailure { exception ->
                update(ItemState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleYesClicked(item: KidObjectiveItem) {
        update(ItemState.Loading)
        item.yesNoList.add(YesNoItem(yes = true, date = Date()))
        val consecutiveYesses = orgRepo.requireSelectedKidObjective().consecutiveYesses
        if (consecutiveYesses != null) {
            val recentEntries = item.yesNoList.takeLast(consecutiveYesses)
            if (recentEntries.size >= consecutiveYesses && recentEntries.all { it.yes }) {
                item.mastered = true
            }
        }
        if (item.firstResponseTime == null) item.firstResponseTime = Date()
        item.lastResponseTime = Date()
        item.lastModificationByUserId = userRepo.requireUserDetails().ownerUid
        viewModelScope.launch {
            val result = kidObjectiveRepo.updateKidObjective(orgRepo.requireSelectedKidObjective())
            result.onSuccess {
                updateItems()
            }.onFailure { exception ->
                update(ItemState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleNoClicked(item: KidObjectiveItem) {
        update(ItemState.Loading)
        item.yesNoList.add(YesNoItem(yes = false, date = Date()))
        item.lastResponseTime = Date()
        if (item.firstResponseTime == null) item.firstResponseTime = Date()
        item.lastModificationByUserId = userRepo.requireUserDetails().ownerUid
        viewModelScope.launch {
            val result = kidObjectiveRepo.updateKidObjective(orgRepo.requireSelectedKidObjective())
            result.onSuccess {
                updateItems()
            }.onFailure { exception ->
                update(ItemState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleFrequencySet(item: KidObjectiveItem, frequency: Int, interval: Int) {
        update(ItemState.Loading)
        item.lastResponseTime = Date()
        if (item.firstResponseTime == null) item.firstResponseTime = Date()
        item.lastModificationByUserId = userRepo.requireUserDetails().ownerUid
        item.frequencyList.add(FrequencyItem(frequency, interval, Date()))
        viewModelScope.launch {
            val result = kidObjectiveRepo.updateKidObjective(orgRepo.requireSelectedKidObjective())
            result.onSuccess {
                updateItems()
            }.onFailure { exception ->
                update(ItemState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleProgressSet(item: KidObjectiveItem, progress: Int) {
        update(ItemState.Loading)
        item.lastResponseTime = Date()
        if (item.firstResponseTime == null) item.firstResponseTime = Date()
        item.lastModificationByUserId = userRepo.requireUserDetails().ownerUid
        item.percentageList.add(ProgressItem(progress, Date()))

        viewModelScope.launch {
            val result = kidObjectiveRepo.updateKidObjective(orgRepo.requireSelectedKidObjective())
            result.onSuccess {
                updateItems()
            }.onFailure { exception ->
                update(ItemState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleSetMastered(item: KidObjectiveItem) {
        update(ItemState.Loading)
        item.mastered = !item.mastered

        if (item.mastered) {
            item.lastResponseTime = Date()
            item.lastModificationByUserId = userRepo.requireUserDetails().ownerUid
        }

        viewModelScope.launch {
            val result = kidObjectiveRepo.updateKidObjective(orgRepo.requireSelectedKidObjective())
            result.onSuccess {
                updateItems()
            }.onFailure { exception ->
                update(ItemState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleCheckmarkClicked(item: KidObjectiveItem) {
        update(ItemState.Loading)
        item.checkmarkList.add(Date())
        item.lastResponseTime = Date()
        if (item.firstResponseTime == null) item.firstResponseTime = Date()
        item.lastModificationByUserId = userRepo.requireUserDetails().ownerUid
        viewModelScope.launch {
            val result = kidObjectiveRepo.updateKidObjective(orgRepo.requireSelectedKidObjective())
            result.onSuccess {
                updateItems()
            }.onFailure { exception ->
                update(ItemState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleRemoveItem(itemToRemove: KidObjectiveItem) {
        update(ItemState.Loading)
        val newItemsList = orgRepo.requireSelectedObjective().itemsList.filter {
            it.normalizedName != itemToRemove.objItem.normalizedName
        }
        val newKidObjectiveItemList = orgRepo.requireSelectedKidObjective().itemsList.filter {
            it.objItem.normalizedName != itemToRemove.objItem.normalizedName
        }
        val newObjective =
            orgRepo.requireSelectedObjective().copy(itemsList = newItemsList.toMutableList())
        val newKidObjective = orgRepo.requireSelectedKidObjective()
            .copy(itemsList = newKidObjectiveItemList.toMutableList())

        viewModelScope.launch {
            viewModelScope.launch {
                val result = objectiveRepo.updateObjective(newObjective)
                result.onSuccess {
                    orgRepo.setSelectedObjective(newObjective, newKidObjective)
                    updateItems()
                }.onFailure { exception ->
                    update(ItemState.Error(exception.message ?: "Error"))
                }
            }
        }
    }
}
