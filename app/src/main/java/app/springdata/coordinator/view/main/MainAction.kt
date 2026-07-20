package app.springdata.coordinator.view.main

sealed class MainAction {
    object Start : MainAction()
    object UpdateAuthStatus: MainAction()
}