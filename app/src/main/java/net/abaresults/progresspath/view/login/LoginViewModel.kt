package net.abaresults.progresspath.view.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepo: UserRepository
)  : ViewModel() {
    private val _state = MutableLiveData<LoginState>()
        .apply { value = LoginState.Idle }
    val state: LiveData<LoginState> = _state

    fun takeAction(action: LoginAction) {
        when (action) {
            is LoginAction.Start -> handleStart()
            is LoginAction.LoginClicked -> handleLoginClicked(action.email, action.password)
        }
    }

    private fun update(newState: LoginState) {
        _state.postValue(newState)
    }

    private fun handleStart() {

    }

    private fun handleLoginClicked(email: String, password: String) {
        if (validateInputs(email, password)) {
            update(LoginState.Loading)
            viewModelScope.launch {
                userRepo.login(email, password)
                    .onSuccess { update(LoginState.LoginSuccess) }
                    .onFailure { update(LoginState.Error(it.message ?: "Unknown error", "", "")) }
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true
        var emailError = ""
        var passwordError = ""

        if (email.isEmpty()) {
            emailError = "Email cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"
            isValid = false
        }

        if (password.isEmpty()) {
           passwordError = "Password cannot be empty"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        }

        update(LoginState.Error("", emailError, passwordError))

        return isValid
    }
}