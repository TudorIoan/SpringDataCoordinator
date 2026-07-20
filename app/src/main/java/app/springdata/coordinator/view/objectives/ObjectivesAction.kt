package app.springdata.coordinator.view.objectives

import app.springdata.coordinator.model.KidObjective

sealed class ObjectivesAction {
    object Start : ObjectivesAction()
    class ObjectiveClicked(val objectiveListItem: ObjectivesListItem) : ObjectivesAction()
    class ToggleObjectiveActive(val kidObjective: KidObjective, val isActive: Boolean) : ObjectivesAction()
    class GenerateObjectiveReport(val kidObjective: KidObjective) : ObjectivesAction()
    class RemoveObjective(val kidObjective: KidObjective) : ObjectivesAction()
    class UpdateMasteryCriteria(val kidObjective: KidObjective, val consecutiveYesses: Int?) : ObjectivesAction()
}