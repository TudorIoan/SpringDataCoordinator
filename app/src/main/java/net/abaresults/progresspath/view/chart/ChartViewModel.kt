package net.abaresults.progresspath.view.chart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.abaresults.progresspath.repo.OrgRepository
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val orgRepo: OrgRepository
) : ViewModel() {

    private val _state = MutableLiveData<ChartState>()
        .apply { value = ChartState.Idle }
    val state: LiveData<ChartState> = _state

    fun takeAction(action: ChartAction) {
        when (action) {
            is ChartAction.Start -> handleStart()
        }
    }

    private fun handleStart() {
        val items = orgRepo.requireSelectedKidObjective().itemsList
        _state.value = ChartState.ContentLoaded(items)
    }
}
