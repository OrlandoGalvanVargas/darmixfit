package com.darmix.darmixfit.ui.edit

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.DarmixFitApp
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.data.RoutineEditing
import com.darmix.darmixfit.data.RoutineRepository
import com.darmix.darmixfit.ui.common.MessageType
import com.darmix.darmixfit.ui.common.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class EditUiState(
    val selectedDay: Int,
    val dayTitle: String = "",
    val entries: List<List<ExerciseEntity>> = emptyList(),
    val loading: Boolean = true,
    val dayCounts: List<Int> = List(7) { 0 }
) {
    val exerciseCount: Int get() = entries.sumOf { it.size }
    val totalSets: Int get() = entries.sumOf { entry -> entry.minOf { it.sets } }
    val supersetCount: Int get() = entries.count { it.size > 1 }
}

class EditRoutineViewModel(private val repo: RoutineRepository) : ViewModel() {

    private val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    val uiState: StateFlow<EditUiState> = combine(
        selectedDay,
        repo.observeDayPlans(),
        repo.observeAllExercises()
    ) { day, plans, exercises ->
        val byDay = exercises.groupBy { it.dayOfWeek }
        EditUiState(
            selectedDay = day,
            dayTitle = plans.firstOrNull { it.dayOfWeek == day }?.title.orEmpty(),
            entries = RoutineEditing.entriesOf(byDay[day].orEmpty()),
            loading = plans.isEmpty(),
            dayCounts = (1..7).map { byDay[it]?.size ?: 0 }
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EditUiState(selectedDay = selectedDay.value)
        )

    fun selectDay(day: Int) {
        selectedDay.value = day
    }

    fun addExercise(name: String, sets: Int, reps: String, note: String) {
        viewModelScope.launch {
            repo.addExercise(selectedDay.value, name, sets, reps, note)
            _messages.tryEmit(UiMessage("«$name» agregado", MessageType.SUCCESS))
        }
    }

    fun saveExercise(exercise: ExerciseEntity, name: String, sets: Int, reps: String, note: String) {
        viewModelScope.launch {
            repo.saveExercise(exercise.id, exercise.dayOfWeek, name, sets, reps, note)
            _messages.tryEmit(UiMessage("Cambios guardados", MessageType.SUCCESS))
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repo.deleteExercise(exercise.id, exercise.dayOfWeek)
            _messages.tryEmit(UiMessage("«${exercise.name}» eliminado", MessageType.SUCCESS))
        }
    }

    fun move(exercise: ExerciseEntity, up: Boolean) {
        viewModelScope.launch { repo.moveEntry(exercise.dayOfWeek, exercise.id, up) }
    }

    fun linkWithNext(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repo.linkWithNext(exercise.dayOfWeek, exercise.id)
            _messages.tryEmit(UiMessage("Superserie creada", MessageType.INFO))
        }
    }

    fun unlink(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repo.unlink(exercise.dayOfWeek, exercise.id)
            _messages.tryEmit(UiMessage("Superserie separada", MessageType.INFO))
        }
    }

    fun updateDayTitle(title: String) {
        viewModelScope.launch { repo.updateDayTitle(selectedDay.value, title) }
    }

    fun setImage(exerciseId: Long, uri: Uri) {
        viewModelScope.launch {
            if (!repo.setExerciseImage(exerciseId, uri)) {
                _messages.tryEmit(UiMessage("No se pudo guardar la imagen", MessageType.ERROR))
            }
        }
    }

    fun restoreImage(exercise: ExerciseEntity) {
        viewModelScope.launch {
            if (repo.restoreBundledImage(exercise.id, exercise.name)) {
                _messages.tryEmit(UiMessage("Imagen incluida restaurada", MessageType.SUCCESS))
            } else {
                _messages.tryEmit(UiMessage("No hay imagen incluida para este ejercicio", MessageType.ERROR))
            }
        }
    }

    fun removeImage(exerciseId: Long) {
        viewModelScope.launch { repo.clearExerciseImage(exerciseId) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DarmixFitApp
                EditRoutineViewModel(app.repository)
            }
        }
    }
}
