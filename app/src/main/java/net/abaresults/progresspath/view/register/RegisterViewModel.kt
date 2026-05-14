package net.abaresults.progresspath.view.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.repo.TherapistRepository
import net.abaresults.progresspath.repo.UserRepository
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val therapistRepo: TherapistRepository
)  : ViewModel() {
    private val _state = MutableLiveData<RegisterState>()
        .apply { value = RegisterState.Idle }
    val state: LiveData<RegisterState> = _state

    var isInvitedTherapist = false

    fun takeAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.Start -> handleStart()
            is RegisterAction.SignUpClicked -> handleSignUpClicked(action.name, action.email, action.password, action.confirmPassword, action.userType)
            is RegisterAction.EmailTyped -> handleEmailTyped(action.email)
        }
    }

    private fun update(newState: RegisterState) {
        _state.postValue(newState)
    }

    private fun handleStart() {

    }

    private fun handleSignUpClicked(name: String, email: String, password: String, confirmPassword: String, userType: UserType) {
        if (validateInputs(name, email, password, confirmPassword)) {
            update(RegisterState.Loading)

            viewModelScope.launch {
                userRepo.register(name, email, password, userType, isInvitedTherapist)
                    .onSuccess { update(RegisterState.RegisterSuccess) }
                    .onFailure { update(RegisterState.Error(it.message ?: "Unknown error", "", "", "", "")) }
            }
        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        var nameError = ""
        var emailError = ""
        var passwordError = ""
        var confirmPasswordError = ""

        if (name.isEmpty()) {
            nameError = "Name cannot be empty"
            isValid = false
        }

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

        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Password cannot be empty"
            isValid = false
        } else if (confirmPassword.length < 6) {
            confirmPasswordError = "Password must be at least 6 characters"
            isValid = false
        }

        if (password != confirmPassword) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        }

        update(RegisterState.Error("", nameError, emailError, passwordError, confirmPasswordError))

        return isValid
    }

    fun handleEmailTyped(email: String) {
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModelScope.launch {
                therapistRepo.isTherapistInvited(email)
                    .onSuccess {
                        isInvitedTherapist = it != null
                        update(RegisterState.InvitedTherapistFound(it))
                    }
                    .onFailure { update(RegisterState.Error(it.message ?: "Unknown error", "", "", "", "")) }
            }
        }
    }
}