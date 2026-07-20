package app.springdata.coordinator.view.report

sealed class ReportAction {
    object Start : ReportAction()
}