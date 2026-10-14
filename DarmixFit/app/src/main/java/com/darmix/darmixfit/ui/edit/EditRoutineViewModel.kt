package com.darmix.darmixfit.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.FitFlowApp
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.data.RoutineEditing
import com.darmix.darmixfit.data.RoutineRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditUiState(
    val selectedDay: Int,
    val dayTitle: String = "",
    /** Cada elemento es un ejercicio suelto (1) o una superserie (2 o más). */
    val entries: List<List<ExerciseEntity>> = emptyList(),
    val loading: Boolean = true,
)

class EditRoutineViewModel(private val repo: RoutineRepository) : ViewModel() {

    private val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Avisos de una sola vez (por ejemplo, un error al guardar una imagen). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val uiState: StateFlow<EditUiState> = combine(
        selectedDay,
        repo.observeDayPlans(),
        repo.observeAllExercises(),
    ) { day, plans, exercises ->
        EditUiState(
            selectedDay = day,
            dayTitle = plans.firstOrNull { it.dayOfWeek == day }?.title.orEmpty(),
            entries = RoutineEditing.entriesOf(exercises.filter { it.dayOfWeek == day }),
            loading = plans.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditUiState(selectedDay = selectedDay.value),
    )

    fun selectDay(day: Int) {
        selectedDay.value = day
    }

    fun addExercise(name: String, sets: Int, reps: String, note: String) {
        viewModelScope.launch { repo.addExercise(selectedDay.value, name, sets, reps, note) }
    }

    fun saveExercise(exercise: ExerciseEntity, name: String, sets: Int, reps: String, note: String) {
        viewModelScope.launch {
            repo.saveExercise(exercise.id, exercise.dayOfWeek, name, sets, reps, note)
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch { repo.deleteExercise(exercise.id, exercise.dayOfWeek) }
    }

    fun move(exercise: ExerciseEntity, up: Boolean) {
        viewModelScope.launch { repo.moveEntry(exercise.dayOfWeek, exercise.id, up) }
    }

    fun linkWithNext(exercise: ExerciseEntity) {
        viewModelScope.launch { repo.linkWithNext(exercise.dayOfWeek, exercise.id) }
    }

    fun unlink(exercise: ExerciseEntity) {
        viewModelScope.launch { repo.unlink(exercise.dayOfWeek, exercise.id) }
    }

    fun updateDayTitle(title: String) {
        viewModelScope.launch { repo.updateDayTitle(selectedDay.value, title) }
    }

    fun setImage(exerciseId: Long, uri: Uri) {
        viewModelScope.launch {
            if (!repo.setExerciseImage(exerciseId, uri)) {
                _messages.tryEmit("No se pudo guardar la imagen")
            }
        }
    }

    fun removeImage(exerciseId: Long) {
        viewModelScope.launch { repo.clearExerciseImage(exerciseId) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitFlowApp
                EditRoutineViewModel(app.repository)
            }
        }
    }
}