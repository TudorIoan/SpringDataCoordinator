package app.springdata.coordinator.view.obj_library.add_objective

import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.ObjectiveType

sealed class AddObjLibraryAction {
    object Start : AddObjLibraryAction()
    class CreateObjectiveClicked(val objectiveType: ObjectiveType, val objectiveName: String, val level: ObjLevel) : AddObjLibraryAction()
}