package app.springdata.coordinator.view.obj_library.add_objective

import app.springdata.coordinator.model.ObjLevel

sealed class AddObjLibraryState {
    object Idle: AddObjLibraryState()
    object Loading : AddObjLibraryState()
    class Error(val error: String) : AddObjLibraryState()
    class ContentLoaded(val objLevel: ObjLevel) : AddObjLibraryState()
    class ObjectiveCreated(val objectiveName: String) : AddObjLibraryState()
}