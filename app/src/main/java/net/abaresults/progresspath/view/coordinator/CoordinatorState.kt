package net.abaresults.progresspath.view.coordinator

import net.abaresults.progresspath.model.Clinic
import net.abaresults.progresspath.model.UserProfile

sealed class CoordinatorState {
        object Idle: CoordinatorState()
        object Loading : CoordinatorState()
        class ContentLoaded(val coordinatorUser: UserProfile) : CoordinatorState()
        class Error(val generalError: String) : CoordinatorState()
}