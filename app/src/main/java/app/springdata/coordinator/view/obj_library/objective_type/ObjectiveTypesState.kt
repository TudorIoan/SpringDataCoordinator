package app.springdata.coordinator.view.obj_library.objective_type

sealed class ObjectiveTypesState {
    object Idle : ObjectiveTypesState()
    object Loading : ObjectiveTypesState()
    data class Error(val error: String) : ObjectiveTypesState()
    data class ContentLoaded(val objectiveTypes: List<ObjectiveType>) : ObjectiveTypesState()
    object ObjectiveTypeAdded : ObjectiveTypesState()
    object ObjectiveTypeDeleted : ObjectiveTypesState()
}