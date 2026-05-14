package net.abaresults.progresspath.view.obj_library.objective_type

import net.abaresults.progresspath.model.ObjLevel

sealed class ObjectiveTypesAction {
    object Start : ObjectiveTypesAction()
    data class LoadObjectiveTypes(val level: ObjLevel) : ObjectiveTypesAction()
    data class AddObjectiveType(val name: String, val level: ObjLevel) : ObjectiveTypesAction()
    data class DeleteObjectiveType(val id: Long) : ObjectiveTypesAction()
}