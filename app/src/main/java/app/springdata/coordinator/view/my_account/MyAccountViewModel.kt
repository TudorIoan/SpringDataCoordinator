package app.springdata.coordinator.view.my_account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.springdata.coordinator.repo.UserRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyAccountViewModel @Inject constructor(private val userRepo: UserRepository) : ViewModel() {
    private val _state = MutableLiveData<MyAccountState>()
        .apply { value = MyAccountState.Idle }
    val state: LiveData<MyAccountState> = _state

    fun takeAction(action: MyAccountAction) {
        when (action) {
            is MyAccountAction.Start -> handleStart()
            is MyAccountAction.LogoutClicked -> handleLogoutClicked()
            is MyAccountAction.SendFeedbackClicked -> handleSendFeedbackClicked(action.message)
        }
    }

    private fun update(newState: MyAccountState) {
        _state.postValue(newState)
    }

    private fun handleStart() {
        val userData = userRepo.requireUserDetails()
        update(MyAccountState.Content(userData.name, userData.email))
    }

    private fun handleLogoutClicked() {
        userRepo.logout()
        update(MyAccountState.LogoutDone)
    }

    private fun handleSendFeedbackClicked(message: String) {
        if (message.isBlank()) {
            update(MyAccountState.FeedbackValidationError("Please write a short message first."))
            return
        }

        update(MyAccountState.Loading)
        viewModelScope.launch {
            userRepo.sendCoordinatorFeedback(message)
                .onSuccess { update(MyAccountState.FeedbackSent) }
                .onFailure { update(MyAccountState.Error(it.message ?: "Unable to send feedback.")) }
        }
    }
}
