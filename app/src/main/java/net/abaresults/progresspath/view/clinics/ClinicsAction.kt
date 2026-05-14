package net.abaresults.progresspath.view.clinics

import net.abaresults.progresspath.model.Clinic

sealed class ClinicsAction {
    object Start : ClinicsAction()
    data class ClinicClicked(val clinic: Clinic) : ClinicsAction()
    data class UpdateClinicName(val clinic: Clinic) : ClinicsAction()
    data class RemoveClinic(val clinic: Clinic) : ClinicsAction()
}