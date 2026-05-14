package net.abaresults.progresspath.view.obj_library

import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.Objective

sealed class ObjLibraryState {
    object Idle: ObjLibraryState()
    object Loading : ObjLibraryState()
    class ContentLoaded(val items: List<ObjLibraryListItem>, val selectedLevel: ObjLevel) : ObjLibraryState()
    class Error(val generalError: String) : ObjLibraryState()
    object GoToAddObjective : ObjLibraryState()
    class GoToItemLibrary(val objective: Objective) : ObjLibraryState()
}