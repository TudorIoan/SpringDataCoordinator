package net.abaresults.progresspath.view.main

import net.abaresults.progresspath.model.UserType

sealed class MainState {
        object Idle: MainState()
        data class AuthStatus(val isLoggedIn: Boolean, val userType: UserType? = null) : MainState()
}