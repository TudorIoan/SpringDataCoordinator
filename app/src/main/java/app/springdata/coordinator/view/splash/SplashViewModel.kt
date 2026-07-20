package app.springdata.coordinator.view.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.BuildConfig
import app.springdata.coordinator.model.UserType
import app.springdata.coordinator.repo.AppSettingsRepository
import app.springdata.coordinator.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val appSettingsRepo: AppSettingsRepository
) : ViewModel() {

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
            appSettingsRepo.fetchAppSettings()
                .onSuccess { settings ->
                    if (settings != null && BuildConfig.VERSION_CODE < settings.minAppVersion) {
                        update(SplashState.UpdateRequired(settings.minAppMessage))
                    } else {
                        continueToApp()
                    }
                }
                .onFailure {
                    update(SplashState.Error(it.message ?: "Unable to check app version."))
                }
        }
    }

    private suspend fun continueToApp() {
        userRepo.autoLogIn()
        if (userRepo.isLoggedIn() && userRepo.requireUserDetails().userType != UserType.COORDINATOR.roleName) {
            userRepo.logout()
        }
        update(SplashState.ContentLoaded)
    }
}
