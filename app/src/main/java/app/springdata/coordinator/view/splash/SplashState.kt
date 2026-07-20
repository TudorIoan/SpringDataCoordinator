package app.springdata.coordinator.view.splash

sealed class SplashState {
    object Idle: SplashState()
    object ContentLoaded: SplashState()
    object Loading : SplashState()
    class UpdateRequired(val message: String) : SplashState()
    class Error(val message: String) : SplashState()
}
