package app.springdata.coordinator.view.obj_library

import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.Objective

sealed class ObjLibraryAction {
    object Start : ObjLibraryAction()
    class ObjectiveClicked(val objectiveListItem: ObjLibraryListItem) : ObjLibraryAction()
    object AddObjectiveClicked : ObjLibraryAction()
    class RemoveObjective(val objective: Objective) : ObjLibraryAction()
    class UpdateObjective(val objective: Objective) : ObjLibraryAction()
    class LevelTabSelected(val level: ObjLevel) : ObjLibraryAction()
}