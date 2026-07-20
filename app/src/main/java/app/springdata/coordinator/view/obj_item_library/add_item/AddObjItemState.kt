package app.springdata.coordinator.view.obj_item_library.add_item

sealed class AddObjItemState {
    object Idle : AddObjItemState()
    object Loading : AddObjItemState()
    class Error(val error: String) : AddObjItemState()
    class ItemAdded(val itemName: String) : AddObjItemState()
}