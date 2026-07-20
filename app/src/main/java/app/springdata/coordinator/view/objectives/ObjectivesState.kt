package app.springdata.coordinator.view.objectives

sealed class ObjectivesState {
        object Idle: ObjectivesState()
        object Loading : ObjectivesState()
        class ContentLoaded(val items: List<ObjectivesListItem>) : ObjectivesState()
        class Error(val generalError: String) : ObjectivesState()
        object GoToItems : ObjectivesState()
        data class GoToReport(val pdfByteArray: ByteArray) : ObjectivesState()
}