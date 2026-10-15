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
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class DayStatus { COMPLETE, PARTIAL }

private const val RECENT_WEEKS = 8

@Immutable
data class HistoryStats(
    val monthComplete: Int = 0,
    val monthPartial: Int = 0,
    val monthSets: Int = 0,
    val weekComplete: Int = 0,
    val weeklyStreak: Int = 0,
    val recentWeeks: List<Boolean> = List(RECENT_WEEKS) { false }
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

    val uiState: StateFlow<HistoryUiState> = combine(
        month,
        selected,
        monthLogs,
        dayDetail,
        repo.observeCompleteDates()
    ) { m, sel, logs, detail, completeDates ->
        val today = LocalDate.now()
        val byDate = logs.groupBy { LocalDate.parse(it.date) }

        val statuses = byDate.mapValues { (_, list) ->
            if (list.any { it.isComplete }) DayStatus.COMPLETE else DayStatus.PARTIAL
        }
        val ratios = byDate.mapValues { (_, list) ->
            list.maxOf { if (it.plannedSets <= 0) 0f else it.doneSets.toFloat() / it.plannedSets }.coerceIn(0f, 1f)
        }

        val weekStart = monday(today)
        val weekEnd = weekStart.plusDays(6)
        val completeWeeks = completeDates.map(::monday).toSet()

        HistoryUiState(
            month = m,
            selected = sel,
            today = today,
            statuses = statuses,
            ratios = ratios,
            dayLogs = detail.first,
            dayExercises = detail.second,
            stats = HistoryStats(
                monthComplete = statuses.count { it.value == DayStatus.COMPLETE },
                monthPartial = statuses.count { it.value == DayStatus.PARTIAL },
                monthSets = logs.sumOf { it.doneSets },
                weekComplete = completeDates.count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) },
                weeklyStreak = weeklyStreak(completeWeeks, weekStart),
                recentWeeks = List(RECENT_WEEKS) { i ->
                    weekStart.minusWeeks((RECENT_WEEKS - 1 - i).toLong()) in completeWeeks
                }
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

    private fun monday(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun weeklyStreak(weeks: Set<LocalDate>, currentWeek: LocalDate): Int {
        var cursor = currentWeek
        if (cursor !in weeks) cursor = cursor.minusWeeks(1)
        var streak = 0
        while (cursor in weeks) {
            streak++
            cursor = cursor.minusWeeks(1)
        }
        return streak
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
