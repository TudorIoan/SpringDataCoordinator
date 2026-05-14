package net.abaresults.progresspath.view.therapists

sealed class TherapistState {
        object Idle: TherapistState()
        object Loading : TherapistState()
        class ContentLoaded(val items: List<TherapistVM>, val isClinic: Boolean) : TherapistState()
        class Error(val generalError: String) : TherapistState()
        object GoToObjectives : TherapistState()
}