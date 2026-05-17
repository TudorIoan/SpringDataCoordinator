package net.abaresults.progresspath.view.obj_item_library.add_item

import net.abaresults.progresspath.model.ObjItemType

sealed class AddObjItemAction {
    data class AddItemClicked(val itemName: String, val itemType: ObjItemType) : AddObjItemAction()
}
