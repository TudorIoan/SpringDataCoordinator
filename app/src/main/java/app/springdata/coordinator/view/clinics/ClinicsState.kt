package app.springdata.coordinator.view.clinics

import app.springdata.coordinator.model.Clinic

sealed class ClinicsState {
        object Idle: ClinicsState()
        object Loading : ClinicsState()
        class ContentLoaded(val clinics: List<Clinic>) : ClinicsState()
        class Error(val generalError: String) : ClinicsState()
        object GoToKids : ClinicsState()
}