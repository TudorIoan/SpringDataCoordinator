package net.abaresults.progresspath.view.obj_item_library

import net.abaresults.progresspath.model.ObjItem

sealed class ObjItemLibraryAction {
    object Start : ObjItemLibraryAction()
    data class UpdateItem(val item: ObjItem) : ObjItemLibraryAction()
    data class RemoveItem(val item: ObjItem) : ObjItemLibraryAction()
}