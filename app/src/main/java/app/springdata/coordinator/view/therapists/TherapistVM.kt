package app.springdata.coordinator.view.therapists

data class TherapistVM(
    val name: String,
    val email: String,
    val isOnlyInvited: Boolean,
)
