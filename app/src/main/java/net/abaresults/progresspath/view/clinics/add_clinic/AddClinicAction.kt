package net.abaresults.progresspath.view.clinics.add_clinic

sealed class AddClinicAction {
    data class AddClinicClicked(val clinicName: String) : AddClinicAction()
}