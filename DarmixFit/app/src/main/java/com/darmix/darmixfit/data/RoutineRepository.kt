package com.darmix.darmixfit.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

class RoutineRepository(context: Context, db: AppDatabase) {

    private val appContext = context.applicationContext
    private val routineDao = db.routineDao()
    private val progressDao = db.progressDao()
    private val programDao = db.programDao()
    private val historyDao = db.historyDao()
    private val catalogDao = db.catalogDao()

    fun observeDayPlans(): Flow<List<DayPlanEntity>> = routineDao.observeDayPlans()

    fun observeAllExercises(): Flow<List<ExerciseEntity>> = routineDao.observeAllExercises()

    fun observeExercises(day: DayOfWeek): Flow<List<ExerciseEntity>> =
        routineDao.observeExercisesByDay(day.value)

    fun observeProgress(date: LocalDate): Flow<Map<Long, Int>> =
        progressDao.observeByDate(date.toString())
            .map { list -> list.associate { it.exerciseId to it.completedSets } }

    suspend fun completeSet(exercise: ExerciseEntity, date: LocalDate): Boolean =
        progressDao.increment(exercise.id, date.toString(), exercise.sets)

    suspend fun completeRound(exercises: List<ExerciseEntity>, date: LocalDate): Boolean =
        progressDao.incrementMany(
            exercises.map { SetTarget(it.id, it.sets) },
            date.toString()
        )

    suspend fun undoSet(exerciseId: Long, date: LocalDate) {
        progressDao.decrement(exerciseId, date.toString())
    }

    suspend fun addExercise(day: Int, name: String, sets: Int, reps: String, note: String) =
        routineDao.addExercise(day, name, sets, reps, note).also {
            ExerciseCatalog.imageRefFor(name)?.let { ref -> catalogDao.assignByName(day, name, ref) }
        }

    suspend fun saveExercise(
        id: Long,
        day: Int,
        name: String,
        sets: Int,
        reps: String,
        note: String,
    ) = routineDao.saveExercise(id, day, name, sets, reps, note)

    suspend fun deleteExercise(id: Long, day: Int) {
        val image = routineDao.getImage(id)
        routineDao.deleteExercise(id, day)
        ExerciseImageStore.delete(appContext, image)
    }

    suspend fun moveEntry(day: Int, exerciseId: Long, up: Boolean) =
        routineDao.moveEntry(day, exerciseId, up)

    suspend fun linkWithNext(day: Int, exerciseId: Long) =
        routineDao.linkWithNext(day, exerciseId)

    suspend fun unlink(day: Int, exerciseId: Long) = routineDao.unlink(day, exerciseId)

    suspend fun updateDayTitle(day: Int, title: String) = routineDao.updateDayTitle(day, title)

    suspend fun setExerciseImage(exerciseId: Long, source: Uri): Boolean {
        val newName = ExerciseImageStore.import(appContext, source) ?: return false
        val old = routineDao.getImage(exerciseId)
        routineDao.updateImage(exerciseId, newName)
        ExerciseImageStore.delete(appContext, old)
        return true
    }

    suspend fun restoreBundledImage(exerciseId: Long, exerciseName: String): Boolean {
        val ref = ExerciseCatalog.imageRefFor(exerciseName) ?: return false
        val old = routineDao.getImage(exerciseId)
        routineDao.updateImage(exerciseId, ref)
        if (old != ref) ExerciseImageStore.delete(appContext, old)
        return true
    }

    suspend fun clearExerciseImage(exerciseId: Long) {
        val old = routineDao.getImage(exerciseId)
        routineDao.updateImage(exerciseId, null)
        ExerciseImageStore.delete(appContext, old)
    }

    fun observePrograms(): Flow<List<ProgramEntity>> = programDao.observePrograms()

    fun observeActiveProgram(): Flow<ProgramEntity?> = programDao.observeActive()

    suspend fun activateProgram(id: Long) = programDao.activate(id)

    suspend fun renameProgram(id: Long, name: String) {
        if (name.isNotBlank()) programDao.rename(id, name.trim())
    }

    suspend fun addProgram(
        template: ProgramTemplate,
        name: String? = null,
        activate: Boolean = true,
    ): String {
        val base = name?.trim().takeUnless { it.isNullOrEmpty() } ?: template.name
        val existing = programDao.names().map { it.lowercase() }.toSet()
        var finalName = base
        var suffix = 2
        while (finalName.lowercase() in existing) {
            finalName = "$base ($suffix)"
            suffix++
        }

        val exercises = template.days.flatMapIndexed { dayIndex, day ->
            day.exercises.mapIndexed { order, item ->
                ExerciseEntity(
                    programId = 0,
                    dayOfWeek = dayIndex + 1,
                    sortOrder = order,
                    name = item.name,
                    sets = item.sets,
                    reps = item.reps,
                    note = item.note,
                    imageUri = ExerciseCatalog.imageRefFor(item.name),
                    supersetGroup = item.group,
                )
            }
        }

        programDao.createProgram(
            program = ProgramEntity(
                name = finalName,
                description = template.summary,
                templateKey = template.key,
                restSeconds = template.restSeconds,
            ),
            dayTitles = template.days.map { it.title },
            exercises = exercises,
            activate = activate,
        )
        return finalName
    }

    suspend fun deleteProgram(id: Long): Boolean {
        if (programDao.count() <= 1) return false
        val images = programDao.deleteProgram(id)
        images.forEach { ExerciseImageStore.delete(appContext, it) }
        return true
    }

    fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<WorkoutLogEntity>> =
        historyDao.observeLogs(from.toString(), to.toString())

    fun observeDayLogs(date: LocalDate): Flow<List<DayLogRow>> =
        historyDao.observeDayLogs(date.toString())

    fun observeDayExercises(date: LocalDate): Flow<List<HistoryExerciseRow>> =
        historyDao.observeDayExercises(date.toString())

    fun observeCompleteDates(): Flow<List<LocalDate>> =
        historyDao.observeCompleteDates().map { list -> list.map { LocalDate.parse(it) } }

    suspend fun seedIfNeeded() {
        if (programDao.count() == 0) {
            addProgram(ProgramTemplates.default, activate = true)
        } else {
            programDao.ensureActive()
        }
        backfillBundledImages()
    }

    private suspend fun backfillBundledImages() {
        val prefs = appContext.getSharedPreferences(CATALOG_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_BACKFILL, false)) return
        val assignments = catalogDao.templateExercisesWithoutImage()
            .mapNotNull { row -> ExerciseCatalog.imageRefFor(row.name)?.let { row.id to it } }
            .toMap()
        if (assignments.isNotEmpty()) catalogDao.assignImages(assignments)
        prefs.edit().putBoolean(KEY_BACKFILL, true).apply()
    }

    private companion object {
        const val CATALOG_PREFS = "darmixfit_catalog"
        const val KEY_BACKFILL = "bundled_images_v1"
    }
}