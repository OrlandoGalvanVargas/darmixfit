package com.darmix.darmixfit.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.FitFlowApp
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.data.RoutineRepository
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

data class ExerciseProgress(val exercise: ExerciseEntity, val completed: Int)

sealed interface TodayItem {
    val key: String

    data class Single(val entry: ExerciseProgress) : TodayItem {
        override val key = "ex-${entry.exercise.id}"
    }

    data class Superset(val members: List<ExerciseProgress>) : TodayItem {
        override val key = "ss-${members.first().exercise.id}"
        val totalRounds: Int get() = members.minOf { it.exercise.sets }
        val completedRounds: Int get() = members.minOf { it.completed }
    }
}

data class TodayUiState(
    val date: LocalDate,
    val planTitle: String? = null,
    val items: List<TodayItem> = emptyList(),
    /** true mientras la rutina precargada todavía se está insertando. */
    val loading: Boolean = true,
)

class TodayViewModel(
    private val repo: RoutineRepository,
    private val appContext: Context,
    /** Solo para pruebas: fuerza una fecha (ej. LocalDate.of(2026, 9, 25) = viernes). */
    private val debugDate: LocalDate? = null,
) : ViewModel() {

    private fun now(): LocalDate = debugDate ?: LocalDate.now()

    private val today = MutableStateFlow(now())

    private val _restVisible = MutableStateFlow(false)
    val restVisible: StateFlow<Boolean> = _restVisible.asStateFlow()

    val timerState: StateFlow<TimerState> = TimerStore.state

    /** Descanso sugerido del programa activo (cada programa puede tener el suyo). */
    val restDefaultSeconds: StateFlow<Int> = repo.observeActiveProgram()
        .map { it?.restSeconds ?: RestTimer.DEFAULT_SECONDS }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RestTimer.DEFAULT_SECONDS,
        )

    init {
        // El modal se abre cuando el temporizador arranca y se cierra solo
        // cuando termina o se cancela (aunque eso ocurra con la app en segundo plano).
        viewModelScope.launch {
            TimerStore.state
                .map { it.active }
                .distinctUntilChanged()
                .collect { active -> _restVisible.value = active }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = today
        .flatMapLatest { date ->
            combine(
                repo.observeDayPlans(),
                repo.observeExercises(date.dayOfWeek),
                repo.observeProgress(date),
            ) { plans, exercises, progress ->
                TodayUiState(
                    date = date,
                    planTitle = plans.firstOrNull { it.dayOfWeek == date.dayOfWeek.value }?.title,
                    items = buildItems(exercises, progress),
                    loading = plans.isEmpty(),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(date = now()),
        )

    /**
     * true cuando TODOS los ejercicios del día están completos. Es un reflejo directo del
     * estado actual, sin memoria de si ya se mostró antes: la pantalla decide cuándo ocultarlo.
     */
    val allDayComplete: StateFlow<Boolean> = uiState
        .map { isAllComplete(it.items) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private fun isAllComplete(items: List<TodayItem>): Boolean =
        items.isNotEmpty() && items.all { item ->
            when (item) {
                is TodayItem.Single -> item.entry.completed >= item.entry.exercise.sets
                is TodayItem.Superset -> item.completedRounds >= item.totalRounds
            }
        }

    /** Agrupa los ejercicios de una misma superserie en un solo elemento. */
    private fun buildItems(
        exercises: List<ExerciseEntity>,
        progress: Map<Long, Int>,
    ): List<TodayItem> {
        val items = mutableListOf<TodayItem>()
        val seenGroups = mutableSetOf<Int>()
        for (exercise in exercises) {
            val group = exercise.supersetGroup
            if (group == null) {
                items += TodayItem.Single(ExerciseProgress(exercise, progress[exercise.id] ?: 0))
            } else if (seenGroups.add(group)) {
                val members = exercises
                    .filter { it.supersetGroup == group }
                    .map { ExerciseProgress(it, progress[it.id] ?: 0) }
                items += TodayItem.Superset(members)
            }
        }
        return items
    }

    fun refreshDate() {
        today.value = now()
        TimerStore.expireIfStale()
    }

    fun completeSet(exercise: ExerciseEntity) {
        viewModelScope.launch {
            if (repo.completeSet(exercise, today.value)) _restVisible.value = true
        }
    }

    fun completeRound(members: List<ExerciseEntity>) {
        viewModelScope.launch {
            if (repo.completeRound(members, today.value)) _restVisible.value = true
        }
    }

    fun undoSet(exercise: ExerciseEntity) {
        viewModelScope.launch { repo.undoSet(exercise.id, today.value) }
    }

    fun undoRound(members: List<ExerciseEntity>) {
        viewModelScope.launch {
            members.forEach { repo.undoSet(it.id, today.value) }
        }
    }

    fun startRest(seconds: Int) = RestTimer.start(appContext, seconds)

    fun cancelRest() = RestTimer.cancel(appContext)

    /** Cierra el modal cuando el temporizador todavía no se inició. */
    fun dismissRest() {
        _restVisible.value = false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitFlowApp
                TodayViewModel(app.repository, app)
            }
        }
    }
}