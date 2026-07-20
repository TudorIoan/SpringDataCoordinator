package app.springdata.coordinator.view.main

sealed class MainState {
        object Idle: MainState()
        data class AuthStatus(val isLoggedIn: Boolean) : MainState()
}
