package net.abaresults.progresspath.view.report

sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    data class ContentLoaded(val pdfData: ByteArray) : ReportState()
    data class Error(val generalError: String) : ReportState()
}