package app.springdata.coordinator.view.my_account

/**
 * Represent an action taken by the user in the Home view
 */
sealed class MyAccountAction {
    object Start : MyAccountAction()
    object LogoutClicked : MyAccountAction()
}