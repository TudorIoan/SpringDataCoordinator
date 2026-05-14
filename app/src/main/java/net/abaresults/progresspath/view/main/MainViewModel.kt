package net.abaresults.progresspath.view.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.repo.UserRepository
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
        val isLoggedIn = userRepo.isLoggedIn()
        val userType = if (isLoggedIn) {
            UserType.fromString(userRepo.requireUserDetails().userType)
        } else null
        update(MainState.AuthStatus(isLoggedIn, userType))
    }
}