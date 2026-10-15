package com.darmix.darmixfit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_HIDE -> SessionNotifier.hide(app)
            ACTION_COMPLETE -> {
                val ids = intent.getLongArrayExtra(EXTRA_IDS) ?: return
                val set = intent.getIntExtra(EXTRA_SET, -1)
                val pending = goAsync()
                scope.launch {
                    try {
                        lock.withLock { complete(app, ids, set) }
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo completar la serie", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun complete(context: Context, ids: LongArray, set: Int) {
        val before = WorkoutSession.snapshot(context)
        val step = before.step
        if (step == null || !step.ids.contentEquals(ids) || step.setNumber != set) {
            SessionNotifier.refreshNow(context)
            return
        }

        val repo = WorkoutSession.repository(context)
        val logged = if (step.isSuperset) {
            repo.completeRound(step.exercises, before.date)
        } else {
            repo.completeSet(step.exercises.first(), before.date)
        }
        if (!logged) {
            SessionNotifier.refreshNow(context)
            return
        }

        val after = WorkoutSession.snapshot(context, before.date)
        if (after.complete) {
            SessionNotifier.cancel(context)
            SessionNotifier.postComplete(context, after)
            return
        }

        SessionNotifier.cancel(context)
        RestTimer.start(context, restSeconds(context))
    }

    private suspend fun restSeconds(context: Context): Int {
        val auto = RestPreferences.get(context).state.value
        if (auto.enabled) return auto.seconds
        val program = runCatching {
            WorkoutSession.repository(context).observeActiveProgram().first()
        }.getOrNull()
        return program?.restSeconds ?: RestTimer.DEFAULT_SECONDS
    }

    companion object {
        const val ACTION_COMPLETE = "com.darmix.darmixfit.action.SESSION_COMPLETE_SET"
        const val ACTION_HIDE = "com.darmix.darmixfit.action.SESSION_HIDE"
        const val EXTRA_IDS = "ids"
        const val EXTRA_SET = "set"

        private const val TAG = "SessionAction"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val lock = Mutex()
    }
}
