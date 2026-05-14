package net.abaresults.progresspath.view.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(val userRepo: UserRepository) : ViewModel() {

    private val _state = MutableLiveData<SplashState>()
        .apply { value = SplashState.Idle }
    val state: LiveData<SplashState> = _state

    fun takeAction(action: SplashAction) {
        when (action) {
            is SplashAction.Start -> handleStart()
        }
    }

    private fun update(newState: SplashState) {
        _state.postValue(newState)
    }

    private fun handleStart() {
        update(SplashState.Loading)
        viewModelScope.launch {
            userRepo.autoLogIn()
            update(SplashState.ContentLoaded)
        }
    }
}