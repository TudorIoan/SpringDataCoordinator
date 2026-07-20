package app.springdata.coordinator.view.obj_item_library.add_item

import app.springdata.coordinator.model.ObjItemType

sealed class AddObjItemAction {
    data class AddItemClicked(val itemName: String, val itemType: ObjItemType) : AddObjItemAction()
}
