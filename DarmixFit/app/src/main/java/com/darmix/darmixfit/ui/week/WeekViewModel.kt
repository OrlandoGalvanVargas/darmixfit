package com.darmix.darmixfit.ui.week

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.DarmixFitApp
import com.darmix.darmixfit.data.DayPlanEntity
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.data.RoutineRepository
import com.darmix.darmixfit.data.WorkoutLogEntity
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
import java.time.temporal.TemporalAdjusters

@Immutable
data class WeekDay(
    val dayOfWeek: Int,
    val date: LocalDate,
    val title: String,
    val entries: List<List<ExerciseEntity>>,
    val totalSets: Int,
    val exerciseCount: Int,
    val supersetCount: Int,
    val doneSets: Int?,
    val plannedSets: Int?
) {
    val isRest: Boolean get() = entries.isEmpty()
    val progress: Float?
        get() = if (doneSets == null || plannedSets == null || plannedSets <= 0) null
        else (doneSets.toFloat() / plannedSets).coerceIn(0f, 1f)
    val isComplete: Boolean get() = (progress ?: 0f) >= 1f
}

@Immutable
data class WeekUiState(
    val days: List<WeekDay> = emptyList(),
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now()
) {
    val weekStart: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val trainingDays: Int = days.count { !it.isRest }
    val restDays: Int = days.count { it.isRest }
    val plannedSets: Int = days.sumOf { it.totalSets }
    val doneSets: Int = days.sumOf { it.doneSets ?: 0 }
    val completedDays: Int = days.count { !it.isRest && it.isComplete }
}

class WeekViewModel(private val repo: RoutineRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WeekUiState> = today
        .flatMapLatest { date ->
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            combine(
                repo.observeDayPlans(),
                repo.observeAllExercises(),
                repo.observeLogs(monday, monday.plusDays(6)),
                repo.observeProgress(date)
            ) { plans, exercises, logs, progress ->
                buildState(date, monday, plans, exercises, logs, progress)
            }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUiState())

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    private fun buildState(
        date: LocalDate,
        monday: LocalDate,
        plans: List<DayPlanEntity>,
        exercises: List<ExerciseEntity>,
        logs: List<WorkoutLogEntity>,
        progress: Map<Long, Int>
    ): WeekUiState {
        val byDay = exercises.groupBy { it.dayOfWeek }
        val logsByDate = logs.groupBy { it.date }.mapValues { (_, list) -> list.maxBy { it.doneSets } }
        val days = plans.sortedBy { it.dayOfWeek }.map { plan ->
            val entries = groupEntries(byDay[plan.dayOfWeek].orEmpty())
            val totalSets = entries.sumOf { entry -> entry.minOf { it.sets } }
            val dayDate = monday.plusDays(plan.dayOfWeek - 1L)
            val (done, planned) = when {
                entries.isEmpty() -> null to null
                dayDate == date -> entries.sumOf { entry ->
                    entry.minOf { progress[it.id] ?: 0 }.coerceAtMost(entry.minOf { it.sets })
                } to totalSets
                dayDate.isBefore(date) -> logsByDate[dayDate.toString()]
                    ?.let { it.doneSets to it.plannedSets }
                    ?: (0 to totalSets)
                else -> null to null
            }
            WeekDay(
                dayOfWeek = plan.dayOfWeek,
                date = dayDate,
                title = plan.title,
                entries = entries,
                totalSets = totalSets,
                exerciseCount = entries.sumOf { it.size },
                supersetCount = entries.count { it.size > 1 },
                doneSets = done,
                plannedSets = planned
            )
        }
        return WeekUiState(days = days, loading = plans.isEmpty(), today = date)
    }

    private fun groupEntries(exercises: List<ExerciseEntity>): List<List<ExerciseEntity>> {
        val groups = exercises.filter { it.supersetGroup != null }.groupBy { it.supersetGroup }
        val seen = HashSet<Int>()
        return buildList {
            for (exercise in exercises) {
                val group = exercise.supersetGroup
                if (group == null) add(listOf(exercise))
                else if (seen.add(group)) add(groups.getValue(group))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DarmixFitApp
                WeekViewModel(app.repository)
            }
        }
    }
}
