package app.springdata.coordinator.view.kids

import app.springdata.coordinator.model.Kid

sealed class KidsState {
        object Idle: KidsState()
        object Loading : KidsState()
        class ContentLoaded(val items: List<Kid>) : KidsState()
        class Error(val generalError: String) : KidsState()
        object GoToObjectives : KidsState()
}
