package com.darmix.darmixfit.ui.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darmix.darmixfit.FitFlowApp
import com.darmix.darmixfit.data.ProgramEntity
import com.darmix.darmixfit.data.ProgramTemplate
import com.darmix.darmixfit.data.RoutineRepository
import com.darmix.darmixfit.ui.common.MessageType
import com.darmix.darmixfit.ui.common.UiMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgramsViewModel(private val repo: RoutineRepository) : ViewModel() {

    val programs: StateFlow<List<ProgramEntity>> = repo.observePrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun activate(id: Long) {
        viewModelScope.launch { repo.activateProgram(id) }
    }

    fun addFromTemplate(template: ProgramTemplate, name: String? = null) {
        viewModelScope.launch {
            val finalName = repo.addProgram(template, name, activate = true)
            _messages.tryEmit(UiMessage("«$finalName» agregado y activado", MessageType.SUCCESS))
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repo.renameProgram(id, name) }
    }

    fun delete(program: ProgramEntity) {
        viewModelScope.launch {
            val deleted = repo.deleteProgram(program.id)
            _messages.tryEmit(
                if (deleted) UiMessage("«${program.name}» eliminado", MessageType.SUCCESS)
                else UiMessage("No puedes eliminar tu único programa", MessageType.ERROR)
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitFlowApp
                ProgramsViewModel(app.repository)
            }
        }
    }
}