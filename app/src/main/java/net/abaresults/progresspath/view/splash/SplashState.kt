package net.abaresults.progresspath.view.splash

sealed class SplashState {
    object Idle: SplashState()
    object ContentLoaded: SplashState()
    object Loading : SplashState()
    class Error(val message: String) : SplashState()
}