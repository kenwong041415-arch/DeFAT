package com.defat.app.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.app.ui.SelectedDayHolder
import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.calc.ExerciseFormula
import com.defat.core.domain.calc.ExercisePreview
import com.defat.core.domain.calc.MetTable
import com.defat.core.domain.repository.ExerciseRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The exercise sheet's form (plan §10.4). The numbers shown come from [preview]. */
data class ExerciseSheetState(
    val activityKey: String = MetTable.ACTIVITIES.first().key,
    /** Which of 輕 / 中 / 重 is chosen; only used while weight training is the activity. */
    val weightTrainingKey: String = MetTable.WEIGHT_TRAINING_KEY,
    val minutesText: String = "",
    /** The student's current weight; null only before the profile has loaded. */
    val weightKg: Double? = null,
) {
    val isWeightTraining: Boolean get() = activityKey == MetTable.WEIGHT_TRAINING_KEY

    /** The key that is stored on the entry: the level key for weight training. */
    val effectiveKey: String get() = if (isWeightTraining) weightTrainingKey else activityKey

    val met: Double?
        get() = if (isWeightTraining) {
            MetTable.WEIGHT_TRAINING_LEVELS.firstOrNull { it.key == weightTrainingKey }?.met
        } else {
            MetTable.metFor(activityKey)
        }

    val minutes: Int? get() = minutesText.toIntOrNull()

    /** `MET x weight x hours = kcal`, computed in :core-domain; null until the inputs are valid. */
    val preview: ExercisePreview?
        get() {
            val weight = weightKg?.takeIf { it > 0.0 } ?: return null
            val mets = met ?: return null
            val mins = minutes ?: return null
            if (!ExerciseFormula.isValidMinutes(mins)) return null
            return ExerciseFormula.preview(mets, weight, mins)
        }

    val canSave: Boolean get() = preview != null
}

@HiltViewModel
class ExerciseEntryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val selectedDay: SelectedDayHolder,
    private val clock: Clock,
) : ViewModel() {

    private val form = MutableStateFlow(ExerciseSheetState())
    private val savedEvents = Channel<Unit>(Channel.BUFFERED)

    /** Emits once after an entry has been written; the sheet closes on it. */
    val saved: Flow<Unit> = savedEvents.receiveAsFlow()

    val uiState: StateFlow<ExerciseSheetState> = combine(form, profileRepository.profile) { current, profile ->
        current.copy(weightKg = profile?.weightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseSheetState())

    /** A fresh form; called when the sheet is opened. */
    fun reset() {
        form.value = ExerciseSheetState()
    }

    fun selectActivity(key: String) = form.update { it.copy(activityKey = key) }

    fun selectWeightTrainingLevel(key: String) = form.update { it.copy(weightTrainingKey = key) }

    /** Digits only, so the minutes field cannot hold anything the formula would reject as text. */
    fun setMinutes(text: String) = form.update { it.copy(minutesText = text.filter { char -> char.isDigit() }.take(MAX_MINUTES_DIGITS)) }

    /**
     * Logs the session on the day Home is showing. `kcal` is computed once, here,
     * from the weight right now, and stored (plan §10.2): a later weight change
     * never rewrites this day.
     */
    fun save() {
        val state = uiState.value
        val preview = state.preview ?: return
        viewModelScope.launch {
            val dayStart = settingsRepository.dayStart.first()
            val now = clock.instant()
            val today = DayBoundary.logicalDate(now, clock.zone, dayStart)
            val date = minOf(selectedDay.selected.value ?: today, today)
            // Today: now. An earlier day: midday of that logical day, which lies
            // inside its window for every day-start the setting allows (00:00..08:00).
            val loggedAt = if (date == today) now else DayBoundary.instantAt(date, LocalTime.NOON, clock.zone, dayStart)
            exerciseRepository.upsert(
                ExerciseFormula.newEntry(
                    id = UUID.randomUUID().toString(),
                    loggedAt = loggedAt,
                    activityKey = state.effectiveKey,
                    met = preview.met,
                    minutes = preview.minutes,
                    weightKg = preview.weightKg,
                ),
            )
            savedEvents.send(Unit)
        }
    }

    private companion object {
        const val MAX_MINUTES_DIGITS = 4
    }
}
