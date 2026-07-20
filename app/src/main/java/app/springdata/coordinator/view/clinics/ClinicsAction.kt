package app.springdata.coordinator.view.clinics

import app.springdata.coordinator.model.Clinic

sealed class ClinicsAction {
    object Start : ClinicsAction()
    data class ClinicClicked(val clinic: Clinic) : ClinicsAction()
    data class UpdateClinicName(val clinic: Clinic) : ClinicsAction()
    data class RemoveClinic(val clinic: Clinic) : ClinicsAction()
}