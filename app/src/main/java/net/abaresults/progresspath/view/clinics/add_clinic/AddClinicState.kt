package net.abaresults.progresspath.view.clinics.add_clinic

sealed class AddClinicState {
        object Idle: AddClinicState()
        object Loading : AddClinicState()
        class Error(val error: String) : AddClinicState()
        object ClinicAdded : AddClinicState()
}