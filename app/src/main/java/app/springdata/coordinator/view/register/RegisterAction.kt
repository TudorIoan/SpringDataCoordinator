package app.springdata.coordinator.view.register

sealed class RegisterAction {
    object Start : RegisterAction()
    class SignUpClicked(
        val name: String,
        val email: String,
        val password: String,
        val confirmPassword: String
    ) : RegisterAction()
}
