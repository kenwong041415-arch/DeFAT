package com.defat.app.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.model.MeasurementSource
import com.defat.core.domain.model.WeightEntry
import com.defat.core.domain.usecase.LogWeightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogWeightUiState(
    val weightKgText: String = "",
    val bodyFatPctText: String = "",
    val date: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false,
) {
    val weightKg: Double? get() = weightKgText.toDoubleOrNull()
    val bodyFatPct: Double? get() = bodyFatPctText.toDoubleOrNull()

    val isSaveEnabled: Boolean
        get() {
            val weightOk = weightKg?.let { it in 30.0..300.0 } == true
            val fatOk = bodyFatPctText.isBlank() || bodyFatPct?.let { it in 3.0..75.0 } == true
            return weightOk && fatOk
        }
}

sealed interface LogWeightEvent {
    data object Done : LogWeightEvent
}

@HiltViewModel
class LogWeightViewModel @Inject constructor(
    private val logWeightUseCase: LogWeightUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogWeightUiState())
    val uiState: StateFlow<LogWeightUiState> = _uiState.asStateFlow()

    private val events = Channel<LogWeightEvent>(Channel.BUFFERED)
    val eventFlow: Flow<LogWeightEvent> = events.receiveAsFlow()

    fun setWeightText(text: String) = _uiState.update { it.copy(weightKgText = text) }
    fun setBodyFatText(text: String) = _uiState.update { it.copy(bodyFatPctText = text) }
    fun setDate(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun save() {
        val state = _uiState.value
        val weightKg = state.weightKg ?: return
        if (!state.isSaveEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            logWeightUseCase(
                WeightEntry(
                    date = state.date,
                    weightKg = weightKg,
                    bodyFatPct = state.bodyFatPct,
                    recordedAt = Instant.now(),
                    source = MeasurementSource.MANUAL,
                ),
            )
            _uiState.update { it.copy(isSaving = false) }
            events.send(LogWeightEvent.Done)
        }
    }
}
