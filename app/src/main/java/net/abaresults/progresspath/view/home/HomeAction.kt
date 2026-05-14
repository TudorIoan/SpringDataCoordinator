package net.abaresults.progresspath.view.home

/**
 * Represent an action taken by the user in the Home view
 */
sealed class HomeAction {
    object Start : HomeAction()
}