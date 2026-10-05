package app.springdata.coordinator.view.items

import app.springdata.coordinator.model.KidObjectiveItem
import app.springdata.coordinator.model.ObjItem

sealed class ItemAction {
    object Start : ItemAction()
    class ItemToggleClicked (val item: KidObjectiveItem): ItemAction()
    class SetMastered (val item: KidObjectiveItem): ItemAction()
    data class RemoveItem(val item: KidObjectiveItem) : ItemAction()
}
