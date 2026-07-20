package app.springdata.coordinator.view.therapists

sealed class TherapistAction {
    object Start : TherapistAction()
    class TherapistClicked (val therapist: TherapistVM): TherapistAction()
    class RemoveTherapist(val therapist: TherapistVM): TherapistAction()
}