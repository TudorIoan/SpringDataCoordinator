package net.abaresults.progresspath.view.items

import net.abaresults.progresspath.model.KidObjectiveItem

sealed class ItemState {
        object Idle: ItemState()
        object Loading : ItemState()
        class ContentLoaded(val items: List<KidObjectiveItem>) : ItemState()
        class Error(val generalError: String) : ItemState()
        object GoToObjectives : ItemState()
}