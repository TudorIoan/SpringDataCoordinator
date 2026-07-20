package app.springdata.coordinator.view.kids.add_kid

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.repo.KidRepository
import app.springdata.coordinator.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class AddKidViewModel @Inject constructor(
    private val kidRepo: KidRepository
)  : ViewModel() {
    private val _state = MutableLiveData<AddKidState>()
        .apply { value = AddKidState.Idle }
    val state: LiveData<AddKidState> = _state

    fun takeAction(action: AddKidAction) {
        when (action) {
            is AddKidAction.AddKidClicked -> handleAddKid(action.kidName)
        }
    }

    private fun update(newState: AddKidState) {
        _state.postValue(newState)
    }

    fun handleAddKid(kidName: String) {
        update(AddKidState.Loading)
        viewModelScope.launch {
            val result = kidRepo.addKid(kidName)
            result.onSuccess { update(AddKidState.KidAdded)
            }.onFailure { exception ->
                update(AddKidState.Error(exception.message ?: "Error"))
            }
        }
    }

}