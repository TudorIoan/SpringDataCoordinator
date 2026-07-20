package app.springdata.coordinator.view.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import app.springdata.coordinator.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val orgRepo: OrgRepository
) : ViewModel() {

    private val _state = MutableLiveData<ReportState>()
        .apply { value = ReportState.Idle }
    val state: LiveData<ReportState> = _state

    fun takeAction(action: ReportAction) {
        when (action) {
            is ReportAction.Start -> handleStart()
        }
    }

    private fun update(newState: ReportState) {
        _state.value = newState
    }

    private fun handleStart() {
        android.util.Log.d("ReportViewModel", "handleStart called")
        update(ReportState.Loading)
        viewModelScope.launch {
            try {
                android.util.Log.d("ReportViewModel", "Trying to get report from orgRepo")
                val reportData = orgRepo.requireCurrentReport()
                android.util.Log.d("ReportViewModel", "Got report data, size: ${reportData.size} bytes")
                update(ReportState.ContentLoaded(reportData))
            } catch (exception: Exception) {
                android.util.Log.e("ReportViewModel", "Error loading report", exception)
                update(ReportState.Error(exception.message ?: "Error loading report"))
            }
        }
    }

}