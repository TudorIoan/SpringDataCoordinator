package net.abaresults.progresspath.view.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor()  : ViewModel() {
    private val _state = MutableLiveData<HomeState>()
        .apply { value = HomeState.Idle }
    val state: LiveData<HomeState> = _state

    fun takeAction(action: HomeAction) {
        when (action) {
            is HomeAction.Start -> handleStart()
        }
    }

    private fun update(newState: HomeState) {
        _state.postValue(newState)
    }

    private fun handleStart() {
    }


}