package com.darmix.darmixfit.ui.today

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.DarmixFitApp
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.data.RoutineRepository
import com.darmix.darmixfit.service.RestAutoState
import com.darmix.darmixfit.service.RestPreferences
import com.darmix.darmixfit.service.RestTimer
import com.darmix.darmixfit.service.TimerState
import com.darmix.darmixfit.service.TimerStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class ExerciseProgress(val exercise: ExerciseEntity, val completed: Int)

@Immutable
sealed interface TodayItem {
    val key: String
    val totalUnits: Int
    val completedUnits: Int
    val isFinished: Boolean get() = completedUnits >= totalUnits

    data class Single(val entry: ExerciseProgress) : TodayItem {
        override val key = "ex-${entry.exercise.id}"
        override val totalUnits = entry.exercise.sets
        override val completedUnits = entry.completed
    }

    data class Superset(val members: List<ExerciseProgress>) : TodayItem {
        override val key = "ss-${members.first().exercise.id}"
        val totalRounds = members.minOf { it.exercise.sets }
        val completedRounds = members.minOf { it.completed }
        override val totalUnits = totalRounds
        override val completedUnits = completedRounds
    }
}

@Immutable
data class TodayUiState(
    val date: LocalDate,
    val planTitle: String? = null,
    val items: List<TodayItem> = emptyList(),
    val loading: Boolean = true,
    val doneCount: Int = 0,
    val activeIndex: Int = -1,
    val completedSets: Int = 0,
    val totalSets: Int = 0
) {
    val totalCount: Int get() = items.size
    val progress: Float get() = if (totalSets == 0) 0f else completedSets.toFloat() / totalSets
    val allComplete: Boolean get() = items.isNotEmpty() && doneCount == items.size
}

class TodayViewModel(
    private val repo: RoutineRepository,
    private val appContext: Context,
    private val debugDate: LocalDate? = null
) : ViewModel() {

    private fun now(): LocalDate = debugDate ?: LocalDate.now()

    private val today = MutableStateFlow(now())

    private val _restVisible = MutableStateFlow(false)
    val restVisible: StateFlow<Boolean> = _restVisible.asStateFlow()

    val timerState: StateFlow<TimerState> = TimerStore.state

    private val restPreferences = RestPreferences.get(appContext)
    val restAuto: StateFlow<RestAutoState> = restPreferences.state

    val restDefaultSeconds: StateFlow<Int> = repo.observeActiveProgram()
        .map { it?.restSeconds ?: RestTimer.DEFAULT_SECONDS }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RestTimer.DEFAULT_SECONDS)

    init {
        viewModelScope.launch {
            TimerStore.state
                .map { it.active }
                .distinctUntilChanged()
                .collect { active -> _restVisible.value = active }
        }
    }

    private val focusOverride = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val baseState = today
        .flatMapLatest { date ->
            combine(
                repo.observeDayPlans(),
                repo.observeExercises(date.dayOfWeek),
                repo.observeProgress(date)
            ) { plans, exercises, progress ->
                val items = buildItems(exercises, progress)
                TodayUiState(
                    date = date,
                    planTitle = plans.firstOrNull { it.dayOfWeek == date.dayOfWeek.value }?.title,
                    items = items,
                    loading = plans.isEmpty(),
                    doneCount = items.count { it.isFinished },
                    activeIndex = items.indexOfFirst { !it.isFinished },
                    completedSets = items.sumOf { it.completedUnits.coerceAtMost(it.totalUnits) },
                    totalSets = items.sumOf { it.totalUnits }
                )
            }
        }

    val uiState: StateFlow<TodayUiState> = combine(baseState, focusOverride) { base, focusKey ->
        val focused = focusKey?.let { key -> base.items.indexOfFirst { it.key == key && !it.isFinished } } ?: -1
        if (focused >= 0) base.copy(activeIndex = focused) else base
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState(date = now()))

    val allDayComplete: StateFlow<Boolean> = uiState
        .map { it.allComplete }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private fun buildItems(exercises: List<ExerciseEntity>, progress: Map<Long, Int>): List<TodayItem> {
        val grouped = exercises.filter { it.supersetGroup != null }.groupBy { it.supersetGroup }
        val seenGroups = HashSet<Int>()
        return buildList {
            for (exercise in exercises) {
                val group = exercise.supersetGroup
                if (group == null) {
                    add(TodayItem.Single(ExerciseProgress(exercise, progress[exercise.id] ?: 0)))
                } else if (seenGroups.add(group)) {
                    val members = grouped.getValue(group).map { ExerciseProgress(it, progress[it.id] ?: 0) }
                    add(TodayItem.Superset(members))
                }
            }
        }
    }

    fun focus(item: TodayItem) {
        focusOverride.value = item.key
    }

    fun refreshDate() {
        val date = now()
        if (date != today.value) focusOverride.value = null
        today.value = date
        TimerStore.expireIfStale()
    }

    fun completeSet(exercise: ExerciseEntity) {
        viewModelScope.launch {
            if (repo.completeSet(exercise, today.value)) onSetLogged()
        }
    }

    fun completeRound(members: List<ExerciseEntity>) {
        viewModelScope.launch {
            if (repo.completeRound(members, today.value)) onSetLogged()
        }
    }

    fun undoSet(exercise: ExerciseEntity) {
        viewModelScope.launch { repo.undoSet(exercise.id, today.value) }
    }

    fun undoRound(members: List<ExerciseEntity>) {
        viewModelScope.launch { members.forEach { repo.undoSet(it.id, today.value) } }
    }

    private fun onSetLogged() {
        val auto = restPreferences.state.value
        if (auto.enabled) {
            RestTimer.start(appContext, auto.seconds)
        } else {
            _restVisible.value = true
        }
    }

    fun startRest(seconds: Int) = RestTimer.start(appContext, seconds)

    fun extendRest() = RestTimer.extend(appContext)

    fun setAutoRest(enabled: Boolean, seconds: Int) = restPreferences.update(enabled, seconds)

    fun cancelRest() = RestTimer.cancel(appContext)

    fun dismissRest() {
        _restVisible.value = false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DarmixFitApp
                TodayViewModel(app.repository, app)
            }
        }
    }
}
