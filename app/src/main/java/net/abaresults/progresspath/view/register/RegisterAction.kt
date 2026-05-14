package net.abaresults.progresspath.view.register

import net.abaresults.progresspath.model.UserType

sealed class RegisterAction {
    object Start : RegisterAction()
    class SignUpClicked(
        val name: String,
        val email: String,
        val password: String,
        val confirmPassword: String,
        val userType: UserType
    ) : RegisterAction()
    class EmailTyped(val email: String) : RegisterAction()
}