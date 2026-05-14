package net.abaresults.progresspath.view.home

sealed class HomeState {
        object Idle: HomeState()
        class Content(val stateData: HomeStateData): HomeState()
        object Loading : HomeState()
        class Error(val message: String) : HomeState()
}