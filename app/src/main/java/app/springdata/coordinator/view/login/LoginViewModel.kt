package app.springdata.coordinator.view.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.model.UserType
import app.springdata.coordinator.repo.UserRepository
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
            is LoginAction.ForgotPasswordClicked -> handleForgotPasswordClicked(action.email)
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
                    .onSuccess {
                        if (userRepo.requireUserDetails().userType == UserType.COORDINATOR.roleName) {
                            update(LoginState.LoginSuccess)
                        } else {
                            userRepo.logout()
                            update(LoginState.Error("This account is not a SpringData Coordinator account.", "", ""))
                        }
                    }
                    .onFailure { update(LoginState.Error(it.message ?: "Unknown error", "", "")) }
            }
        }
    }

    private fun handleForgotPasswordClicked(email: String) {
        if (validateEmail(email)) {
            update(LoginState.Loading)
            viewModelScope.launch {
                userRepo.sendPasswordResetEmail(email)
                    .onSuccess { update(LoginState.PasswordResetEmailSent) }
                    .onFailure { update(LoginState.Error(it.message ?: "Unable to send password reset email", "", "")) }
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true
        val emailError = getEmailError(email)
        var passwordError = ""

        if (emailError.isNotEmpty()) {
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

    private fun validateEmail(email: String): Boolean {
        val emailError = getEmailError(email)

        update(LoginState.Error("", emailError, ""))

        return emailError.isEmpty()
    }

    private fun getEmailError(email: String): String {
        return when {
            email.isEmpty() -> "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address"
            else -> ""
        }
    }
}
