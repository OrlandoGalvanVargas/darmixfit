package com.darmix.darmixfit.service

import android.content.Context
import com.darmix.darmixfit.DarmixFitApp
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.data.RoutineRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class SessionStep(
    val exercises: List<ExerciseEntity>,
    val setNumber: Int,
    val totalSets: Int,
    val nextName: String?
) {
    val isSuperset: Boolean get() = exercises.size > 1
    val ids: LongArray get() = exercises.map { it.id }.toLongArray()
    val title: String get() = exercises.joinToString(" + ") { it.name }
    val reps: String get() = exercises.joinToString(" · ") { "${it.reps} reps" }
}

data class SessionSnapshot(
    val date: LocalDate,
    val step: SessionStep?,
    val doneSets: Int,
    val totalSets: Int
) {
    val started: Boolean get() = doneSets > 0
    val complete: Boolean get() = totalSets > 0 && step == null
}

object WorkoutSession {

    private class Block(val members: List<ExerciseEntity>, val done: Int) {
        val total: Int = members.minOf { it.sets }
        val finished: Boolean get() = done >= total
        val name: String get() = members.joinToString(" + ") { it.name }
    }

    fun repository(context: Context): RoutineRepository =
        (context.applicationContext as DarmixFitApp).repository

    suspend fun snapshot(context: Context, date: LocalDate = LocalDate.now()): SessionSnapshot {
        val repo = repository(context)
        val exercises = repo.observeExercises(date.dayOfWeek).first()
        val progress = repo.observeProgress(date).first()
        val blocks = blocksOf(exercises, progress)
        val total = blocks.sumOf { it.total }
        val done = blocks.sumOf { it.done.coerceAtMost(it.total) }
        val index = blocks.indexOfFirst { !it.finished }
        val step = if (index < 0) null else {
            val block = blocks[index]
            SessionStep(
                exercises = block.members,
                setNumber = block.done + 1,
                totalSets = block.total,
                nextName = blocks.drop(index + 1).firstOrNull { !it.finished }?.name
            )
        }
        return SessionSnapshot(date, step, done, total)
    }

    private fun blocksOf(exercises: List<ExerciseEntity>, progress: Map<Long, Int>): List<Block> {
        val grouped = exercises.filter { it.supersetGroup != null }.groupBy { it.supersetGroup }
        val seen = HashSet<Int>()
        return buildList {
            for (exercise in exercises) {
                val group = exercise.supersetGroup
                if (group == null) {
                    add(Block(listOf(exercise), progress[exercise.id] ?: 0))
                } else if (seen.add(group)) {
                    val members = grouped.getValue(group)
                    add(Block(members, members.minOf { progress[it.id] ?: 0 }))
                }
            }
        }.filter { it.total > 0 }
    }
}
