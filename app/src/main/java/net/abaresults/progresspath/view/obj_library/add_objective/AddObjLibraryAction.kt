package net.abaresults.progresspath.view.obj_library.add_objective

import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.ObjectiveType

sealed class AddObjLibraryAction {
    object Start : AddObjLibraryAction()
    class CreateObjectiveClicked(val objectiveType: ObjectiveType, val objectiveName: String, val level: ObjLevel) : AddObjLibraryAction()
}