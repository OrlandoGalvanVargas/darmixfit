package com.darmix.darmixfit.ui.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.DarmixFitApp
import com.darmix.darmixfit.data.DayLogRow
import com.darmix.darmixfit.data.HistoryExerciseRow
import com.darmix.darmixfit.data.RoutineRepository
import com.darmix.darmixfit.data.isComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

enum class DayStatus { COMPLETE, PARTIAL }

@Immutable
data class HistoryStats(
    val monthComplete: Int = 0,
    val monthSets: Int = 0,
    val compliance: Int? = null
)

@Immutable
data class HistoryUiState(
    val month: YearMonth,
    val selected: LocalDate,
    val today: LocalDate,
    val statuses: Map<LocalDate, DayStatus> = emptyMap(),
    val ratios: Map<LocalDate, Float> = emptyMap(),
    val dayLogs: List<DayLogRow> = emptyList(),
    val dayExercises: List<HistoryExerciseRow> = emptyList(),
    val stats: HistoryStats = HistoryStats()
) {
    val canGoNext: Boolean get() = month < YearMonth.from(today)
    val showToday: Boolean get() = month != YearMonth.from(today) || selected != today
}

class HistoryViewModel(repo: RoutineRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthLogs = month.flatMapLatest { m ->
        repo.observeLogs(m.atDay(1), m.atEndOfMonth())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dayDetail = selected.flatMapLatest { date ->
        combine(repo.observeDayLogs(date), repo.observeDayExercises(date)) { logs, exercises ->
            logs to exercises
        }
    }

    private val trainingDays = repo.observeAllExercises()
        .map { exercises -> exercises.map { it.dayOfWeek }.toSet() }
        .distinctUntilChanged()

    val uiState: StateFlow<HistoryUiState> = combine(
        month,
        selected,
        monthLogs,
        dayDetail,
        trainingDays
    ) { m, sel, logs, detail, training ->
        val today = LocalDate.now()
        val byDate = logs.groupBy { LocalDate.parse(it.date) }

        val statuses = byDate.mapValues { (_, list) ->
            if (list.any { it.isComplete }) DayStatus.COMPLETE else DayStatus.PARTIAL
        }
        val ratios = byDate.mapValues { (_, list) ->
            list.maxOf { if (it.plannedSets <= 0) 0f else it.doneSets.toFloat() / it.plannedSets }.coerceIn(0f, 1f)
        }
        val monthComplete = statuses.count { it.value == DayStatus.COMPLETE }
        val lastDay = if (YearMonth.from(today) == m) today.dayOfMonth else if (m < YearMonth.from(today)) m.lengthOfMonth() else 0
        val plannedDays = (1..lastDay).count { day -> m.atDay(day).dayOfWeek.value in training }

        HistoryUiState(
            month = m,
            selected = sel,
            today = today,
            statuses = statuses,
            ratios = ratios,
            dayLogs = detail.first,
            dayExercises = detail.second,
            stats = HistoryStats(
                monthComplete = monthComplete,
                monthSets = logs.sumOf { it.doneSets },
                compliance = if (plannedDays > 0) (monthComplete * 100 / plannedDays).coerceAtMost(100) else null
            )
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(
                month = YearMonth.now(),
                selected = LocalDate.now(),
                today = LocalDate.now()
            )
        )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        if (month.value < YearMonth.now()) month.value = month.value.plusMonths(1)
    }

    fun select(date: LocalDate) {
        selected.value = date
    }

    fun goToToday() {
        month.value = YearMonth.now()
        selected.value = LocalDate.now()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DarmixFitApp
                HistoryViewModel(app.repository)
            }
        }
    }
}
