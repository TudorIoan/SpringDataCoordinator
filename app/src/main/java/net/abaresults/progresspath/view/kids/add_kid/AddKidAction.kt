package net.abaresults.progresspath.view.kids.add_kid

sealed class AddKidAction {
    data class AddKidClicked(val kidName: String) : AddKidAction()
}