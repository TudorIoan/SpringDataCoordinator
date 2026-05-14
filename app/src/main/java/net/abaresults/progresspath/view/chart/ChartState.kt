package net.abaresults.progresspath.view.chart

import net.abaresults.progresspath.model.KidObjectiveItem

sealed class ChartState {
    object Idle : ChartState()
    class ContentLoaded(val items: List<KidObjectiveItem>) : ChartState()
}
