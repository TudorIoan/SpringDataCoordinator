package net.abaresults.progresspath.view.obj_item_library

import net.abaresults.progresspath.model.ObjItem

sealed class ObjItemLibraryState {
    object Idle: ObjItemLibraryState()
    object Loading : ObjItemLibraryState()
    class ContentLoaded(val items: List<ObjItem>) : ObjItemLibraryState()
    class Error(val generalError: String) : ObjItemLibraryState()
    object GoToObjLibrary : ObjItemLibraryState()
}