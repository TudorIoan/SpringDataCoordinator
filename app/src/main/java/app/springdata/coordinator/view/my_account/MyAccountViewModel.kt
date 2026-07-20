package app.springdata.coordinator.view.my_account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import app.springdata.coordinator.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class MyAccountViewModel @Inject constructor(private val userRepo: UserRepository) : ViewModel() {
    private val _state = MutableLiveData<MyAccountState>()
        .apply { value = MyAccountState.Idle }
    val state: LiveData<MyAccountState> = _state

    fun takeAction(action: MyAccountAction) {
        when (action) {
            is MyAccountAction.Start -> handleStart()
            is MyAccountAction.LogoutClicked -> handleLogoutClicked()        }
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
}
