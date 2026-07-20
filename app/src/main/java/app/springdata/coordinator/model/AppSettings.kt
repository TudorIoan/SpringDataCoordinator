package app.springdata.coordinator.model

data class AppSettings(
    val minAppVersion: Int = 0,
    val minAppMessage: String = ""
)
