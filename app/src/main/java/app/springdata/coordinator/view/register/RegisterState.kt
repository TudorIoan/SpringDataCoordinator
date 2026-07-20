package app.springdata.coordinator.view.register

sealed class RegisterState {
        object Idle: RegisterState()
        object Loading : RegisterState()
        object RegisterSuccess : RegisterState()
        class Error(val generalError: String, val nameError: String, val emailError: String, val passwordError: String, val confirmPasswordError: String) : RegisterState()
}
