package app.springdata.coordinator.view.clinics.add_clinic

sealed class AddClinicAction {
    data class AddClinicClicked(val clinicName: String) : AddClinicAction()
}