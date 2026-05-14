package net.abaresults.progresspath.view.register

import net.abaresults.progresspath.model.UserProfile

sealed class RegisterState {
        object Idle: RegisterState()
        object Loading : RegisterState()
        object RegisterSuccess : RegisterState()
        class InvitedTherapistFound(val coordinatorName: String?) : RegisterState()
        class Error(val generalError: String, val nameError: String, val emailError: String, val passwordError: String, val confirmPasswordError: String) : RegisterState()
}