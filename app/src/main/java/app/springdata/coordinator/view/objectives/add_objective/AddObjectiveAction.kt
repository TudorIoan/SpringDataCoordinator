package app.springdata.coordinator.view.objectives.add_objective

import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.Objective
import app.springdata.coordinator.model.ObjectiveType

sealed class AddObjectiveAction {
    object Start : AddObjectiveAction()
    class ObjLevelChanged(val level: ObjLevel) : AddObjectiveAction()
    class ObjectiveTypeChanged(val objectiveType: ObjectiveType) : AddObjectiveAction()
    class ObjectiveSelected(val objective: Objective, val consecutiveYesses: Int?) : AddObjectiveAction()
}
