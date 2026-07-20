package app.springdata.coordinator.view.my_account

sealed class MyAccountState {
        object Idle: MyAccountState()
        class Content(val name: String, val email: String): MyAccountState()
        object Loading : MyAccountState()
        object LogoutDone : MyAccountState()
        class Error(val message: String) : MyAccountState()
}
