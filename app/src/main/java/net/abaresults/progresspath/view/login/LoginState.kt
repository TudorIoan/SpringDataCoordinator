package net.abaresults.progresspath.view.login

sealed class LoginState {
        object Idle: LoginState()
        object Loading : LoginState()
        object LoginSuccess : LoginState()
        class Error(val generalError: String, val emailError: String, val passwordError: String) : LoginState()
}