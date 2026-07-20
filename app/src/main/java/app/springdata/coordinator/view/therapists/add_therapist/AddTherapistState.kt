package app.springdata.coordinator.view.therapists.add_therapist

sealed class AddTherapistState {
        object Idle: AddTherapistState()
        object Loading : AddTherapistState()
        class Error(val error: String) : AddTherapistState()
        object TherapistAdded : AddTherapistState()
}