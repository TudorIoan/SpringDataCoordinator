package net.abaresults.progresspath.view.kids

import net.abaresults.progresspath.model.Kid
import net.abaresults.progresspath.model.UserType

sealed class KidsState {
        object Idle: KidsState()
        object Loading : KidsState()
        class ContentLoaded(val items: List<Kid>, val userType: UserType) : KidsState()
        class Error(val generalError: String) : KidsState()
        object GoToObjectives : KidsState()
}
