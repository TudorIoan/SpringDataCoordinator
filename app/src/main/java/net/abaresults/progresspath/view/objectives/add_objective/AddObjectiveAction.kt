package net.abaresults.progresspath.view.objectives.add_objective

import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.Objective
import net.abaresults.progresspath.model.ObjectiveType

sealed class AddObjectiveAction {
    object Start : AddObjectiveAction()
    class ObjLevelChanged(val level: ObjLevel) : AddObjectiveAction()
    class ObjectiveTypeChanged(val objectiveType: ObjectiveType) : AddObjectiveAction()
    class ObjectiveSelected(val objective: Objective) : AddObjectiveAction()
}