package app.springdata.coordinator.view.kids

import app.springdata.coordinator.model.Kid

sealed class KidsAction {
    object Start : KidsAction()
    class KidClicked (val kid: Kid): KidsAction()
    class RemoveKid(val kid: Kid): KidsAction()
    class UpdateKidName(val kid: Kid): KidsAction()
}
