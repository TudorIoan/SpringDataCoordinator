package app.springdata.coordinator.view.obj_item_library

import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.ObjItem

data class ObjectiveKidListItem(
    val kidName: String,
    val clinicName: String
)

sealed class ObjItemLibraryState {
    object Idle: ObjItemLibraryState()
    object Loading : ObjItemLibraryState()
    class ContentLoaded(val items: List<ObjItem>, val level: ObjLevel) : ObjItemLibraryState()
    class ObjectiveKidsLoaded(val kids: List<ObjectiveKidListItem>) : ObjItemLibraryState()
    class Error(val generalError: String) : ObjItemLibraryState()
    object GoToObjLibrary : ObjItemLibraryState()
}
