package app.springdata.coordinator.view.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import app.springdata.coordinator.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel@Inject constructor(
    private val userRepo: UserRepository
) : ViewModel() {
    private val _state = MutableLiveData<MainState>()
        .apply { value = MainState.Idle }
    val state: LiveData<MainState> = _state

    fun takeAction(action: MainAction) {
        when (action) {
            is MainAction.Start -> handleStart()
            is MainAction.UpdateAuthStatus -> updateAuthStatus()
        }
    }

    private fun update(newState: MainState) {
        _state.postValue(newState)
    }

    private fun handleStart() {
        updateAuthStatus()
    }

    private fun updateAuthStatus() {
        update(MainState.AuthStatus(userRepo.isLoggedIn()))
    }
}
