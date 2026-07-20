package app.springdata.coordinator.view.therapists.add_therapist

sealed class AddTherapistAction {
    data class AddTherapistClicked(val therapistEmail: String) : AddTherapistAction()
}