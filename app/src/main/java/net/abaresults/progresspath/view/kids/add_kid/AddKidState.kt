package net.abaresults.progresspath.view.kids.add_kid

sealed class AddKidState {
        object Idle: AddKidState()
        object Loading : AddKidState()
        class Error(val error: String) : AddKidState()
        object KidAdded : AddKidState()
}