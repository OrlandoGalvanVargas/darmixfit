package com.darmix.darmixfit.ui.update

import android.app.Application
import android.app.DownloadManager
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darmix.darmixfit.data.update.ApkUpdater
import com.darmix.darmixfit.data.update.ReleaseInfo
import com.darmix.darmixfit.data.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
sealed interface UpdateState {
    data object Hidden : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val info: ReleaseInfo, val progress: Float?) : UpdateState
    data class Ready(val info: ReleaseInfo, val uri: Uri) : UpdateState
    data class Failed(val info: ReleaseInfo) : UpdateState
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    val currentVersion: String = UpdateChecker.currentVersion(application)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Hidden)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _minimized = MutableStateFlow(false)
    val minimized: StateFlow<Boolean> = _minimized.asStateFlow()

    private var downloadJob: Job? = null
    private var downloadId: Long? = null

    init {
        viewModelScope.launch {
            val info = UpdateChecker.latest(app) ?: return@launch
            if (UpdateChecker.shouldPrompt(app, info)) _state.value = UpdateState.Available(info)
        }
    }

    fun later() {
        val info = infoOrNull() ?: return
        UpdateChecker.dismiss(app, info)
        downloadJob?.cancel()
        downloadId?.let { ApkUpdater.cancel(app, it) }
        downloadId = null
        _state.value = UpdateState.Hidden
    }

    fun minimize() {
        _minimized.value = true
    }

    fun download() {
        val info = infoOrNull() ?: return
        if (info.apkUrl == null) return
        downloadJob?.cancel()
        _minimized.value = false
        _state.value = UpdateState.Downloading(info, null)
        downloadJob = viewModelScope.launch {
            val id = try {
                withContext(Dispatchers.IO) { ApkUpdater.enqueue(app, info) }
            } catch (_: Exception) {
                _state.value = UpdateState.Failed(info)
                return@launch
            }
            downloadId = id
            while (isActive) {
                val progress = withContext(Dispatchers.IO) { ApkUpdater.progress(app, id) }
                when (progress?.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val uri = withContext(Dispatchers.IO) { ApkUpdater.uriFor(app, id) }
                        _minimized.value = false
                        _state.value = if (uri != null) UpdateState.Ready(info, uri) else UpdateState.Failed(info)
                        return@launch
                    }

                    DownloadManager.STATUS_FAILED, null -> {
                        _minimized.value = false
                        _state.value = UpdateState.Failed(info)
                        return@launch
                    }

                    else -> _state.value = UpdateState.Downloading(info, progress.fraction)
                }
                delay(POLL_MS)
            }
        }
    }

    private fun infoOrNull(): ReleaseInfo? = when (val s = _state.value) {
        is UpdateState.Available -> s.info
        is UpdateState.Downloading -> s.info
        is UpdateState.Ready -> s.info
        is UpdateState.Failed -> s.info
        UpdateState.Hidden -> null
    }

    private companion object {
        const val POLL_MS = 350L
    }
}
