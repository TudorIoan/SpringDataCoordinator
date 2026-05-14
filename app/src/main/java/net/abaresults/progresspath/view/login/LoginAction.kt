package net.abaresults.progresspath.view.login

sealed class LoginAction {
    object Start : LoginAction()
    class LoginClicked(val email: String, val password: String) : LoginAction()
}