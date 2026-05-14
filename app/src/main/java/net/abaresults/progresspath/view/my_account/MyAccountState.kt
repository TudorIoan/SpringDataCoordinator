package net.abaresults.progresspath.view.my_account

import net.abaresults.progresspath.model.UserType

sealed class MyAccountState {
        object Idle: MyAccountState()
        class Content(val name: String, val email: String, val userType: UserType): MyAccountState()
        object Loading : MyAccountState()
        object LogoutDone : MyAccountState()
        class Error(val message: String) : MyAccountState()
}