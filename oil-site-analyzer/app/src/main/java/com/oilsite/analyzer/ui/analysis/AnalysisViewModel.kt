package com.oilsite.analyzer.ui.analysis

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.oilsite.analyzer.data.model.AnalysisReport
import com.oilsite.analyzer.data.repository.AnalysisRepository
import kotlinx.coroutines.launch

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository(application)

    private val _state = MutableLiveData<AnalysisState>(AnalysisState.Idle)
    val state: LiveData<AnalysisState> = _state

    val hasApiKey: Boolean get() = repository.hasApiKey()

    fun analyze(imageUriString: String) {
        _state.value = AnalysisState.Loading
        viewModelScope.launch {
            val uri = Uri.parse(imageUriString)
            repository.analyzeImage(uri).fold(
                onSuccess = { report -> _state.value = AnalysisState.Success(report) },
                onFailure = { error -> _state.value = AnalysisState.Error(error.message ?: "Unknown error") }
            )
        }
    }
}

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    data class Success(val report: AnalysisReport) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}
