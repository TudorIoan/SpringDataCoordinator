package net.abaresults.progresspath.view.main

sealed class MainAction {
    object Start : MainAction()
    object UpdateAuthStatus: MainAction()
}