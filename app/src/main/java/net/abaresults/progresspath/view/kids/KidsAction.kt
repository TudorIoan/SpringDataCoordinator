package net.abaresults.progresspath.view.kids

import net.abaresults.progresspath.model.Kid

sealed class KidsAction {
    object Start : KidsAction()
    class KidClicked (val kid: Kid): KidsAction()
    class RemoveKid(val kid: Kid): KidsAction()
    class UpdateKidName(val kid: Kid): KidsAction()
    class GenerateKidWorksheet(val kid: Kid): KidsAction()
}