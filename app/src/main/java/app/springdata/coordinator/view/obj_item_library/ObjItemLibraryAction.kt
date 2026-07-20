package app.springdata.coordinator.view.obj_item_library

import app.springdata.coordinator.model.ObjItem

sealed class ObjItemLibraryAction {
    object Start : ObjItemLibraryAction()
    data class UpdateItem(val item: ObjItem) : ObjItemLibraryAction()
    data class RemoveItem(val item: ObjItem) : ObjItemLibraryAction()
}