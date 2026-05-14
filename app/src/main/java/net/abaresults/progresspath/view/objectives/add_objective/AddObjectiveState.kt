package net.abaresults.progresspath.view.objectives.add_objective

sealed class AddObjectiveState {
        object Idle: AddObjectiveState()
        object Loading : AddObjectiveState()
        class Error(val error: String) : AddObjectiveState()
        object ContentLoaded : AddObjectiveState()
        class ObjectiveAdded(val objectiveName: String) : AddObjectiveState()
}