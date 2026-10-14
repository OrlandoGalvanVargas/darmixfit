package com.darmix.darmixfit.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.FitFlowApp
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.data.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WeekDay(
    val dayOfWeek: Int,
    val title: String,
    /** Cada elemento es un ejercicio suelto (1 elemento) o una superserie (2 o más). */
    val entries: List<List<ExerciseEntity>>,
)

data class WeekUiState(
    val days: List<WeekDay> = emptyList(),
    val loading: Boolean = true,
)

class WeekViewModel(repo: RoutineRepository) : ViewModel() {

    val uiState: StateFlow<WeekUiState> = combine(
        repo.observeDayPlans(),
        repo.observeAllExercises(),
    ) { plans, exercises ->
        WeekUiState(
            loading = plans.isEmpty(),
            days = plans.map { plan ->
                WeekDay(
                    dayOfWeek = plan.dayOfWeek,
                    title = plan.title,
                    entries = groupEntries(exercises.filter { it.dayOfWeek == plan.dayOfWeek }),
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeekUiState(),
    )

    private fun groupEntries(exercises: List<ExerciseEntity>): List<List<ExerciseEntity>> {
        val entries = mutableListOf<List<ExerciseEntity>>()
        val seenGroups = mutableSetOf<Int>()
        for (exercise in exercises) {
            val group = exercise.supersetGroup
            if (group == null) {
                entries += listOf(exercise)
            } else if (seenGroups.add(group)) {
                entries += exercises.filter { it.supersetGroup == group }
            }
        }
        return entries
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitFlowApp
                WeekViewModel(app.repository)
            }
        }
    }
}