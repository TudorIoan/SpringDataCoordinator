package net.abaresults.progresspath.view.obj_library

import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.Objective

sealed class ObjLibraryAction {
    object Start : ObjLibraryAction()
    class ObjectiveClicked(val objectiveListItem: ObjLibraryListItem) : ObjLibraryAction()
    object AddObjectiveClicked : ObjLibraryAction()
    class RemoveObjective(val objective: Objective) : ObjLibraryAction()
    class UpdateObjective(val objective: Objective) : ObjLibraryAction()
    class LevelTabSelected(val level: ObjLevel) : ObjLibraryAction()
}