package app.springdata.coordinator.view.chart

import app.springdata.coordinator.model.KidObjectiveItem

sealed class ChartState {
    object Idle : ChartState()
    class ContentLoaded(val items: List<KidObjectiveItem>) : ChartState()
}
