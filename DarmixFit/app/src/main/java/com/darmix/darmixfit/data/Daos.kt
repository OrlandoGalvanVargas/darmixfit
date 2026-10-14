package com.darmix.darmixfit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Subconsulta que devuelve el id del programa activo. */
private const val ACTIVE = "(SELECT id FROM programs WHERE isActive = 1 LIMIT 1)"

/** Todo lo que lee o edita la rutina, siempre del programa activo. */
@Dao
abstract class RoutineDao {

    @Query("SELECT * FROM day_plans WHERE programId = $ACTIVE ORDER BY dayOfWeek")
    abstract fun observeDayPlans(): Flow<List<DayPlanEntity>>

    @Query("SELECT * FROM exercises WHERE programId = $ACTIVE ORDER BY dayOfWeek, sortOrder")
    abstract fun observeAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE programId = $ACTIVE AND dayOfWeek = :day ORDER BY sortOrder")
    abstract fun observeExercisesByDay(day: Int): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE programId = $ACTIVE AND dayOfWeek = :day ORDER BY sortOrder, id")
    abstract suspend fun getByDay(day: Int): List<ExerciseEntity>

    @Query("SELECT id FROM programs WHERE isActive = 1 LIMIT 1")
    abstract suspend fun activeProgramId(): Long?

    @Insert
    abstract suspend fun insertExercise(exercise: ExerciseEntity): Long

    /** UPDATE (no REPLACE): así nunca se borra el progreso por la clave foránea. */
    @Update
    abstract suspend fun updateExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("UPDATE day_plans SET title = :title WHERE dayOfWeek = :day AND programId = $ACTIVE")
    abstract suspend fun updateDayTitle(day: Int, title: String)

    @Query("SELECT imageUri FROM exercises WHERE id = :id")
    abstract suspend fun getImage(id: Long): String?

    @Query("UPDATE exercises SET imageUri = :imageUri WHERE id = :id")
    abstract suspend fun updateImage(id: Long, imageUri: String?)

    @Transaction
    open suspend fun addExercise(day: Int, name: String, sets: Int, reps: String, note: String) {
        val programId = activeProgramId() ?: return
        val nextOrder = (getByDay(day).maxOfOrNull { it.sortOrder } ?: -1) + 1
        insertExercise(
            ExerciseEntity(
                programId = programId,
                dayOfWeek = day,
                sortOrder = nextOrder,
                name = name,
                sets = sets,
                reps = reps,
                note = note,
            )
        )
    }

    /** Si el ejercicio está en una superserie, sus series se propagan a todo el grupo. */
    @Transaction
    open suspend fun saveExercise(
        id: Long,
        day: Int,
        name: String,
        sets: Int,
        reps: String,
        note: String,
    ) {
        val list = getByDay(day)
        val group = list.firstOrNull { it.id == id }?.supersetGroup
        val updated = list.map { e ->
            when {
                e.id == id -> e.copy(name = name, sets = sets, reps = reps, note = note)
                group != null && e.supersetGroup == group -> e.copy(sets = sets)
                else -> e
            }
        }
        updateExercises(updated)
    }

    @Transaction
    open suspend fun deleteExercise(id: Long, day: Int) {
        deleteById(id)
        updateExercises(RoutineEditing.normalize(getByDay(day)))
    }

    @Transaction
    open suspend fun moveEntry(day: Int, exerciseId: Long, up: Boolean) {
        updateExercises(RoutineEditing.move(getByDay(day), exerciseId, up))
    }

    @Transaction
    open suspend fun linkWithNext(day: Int, exerciseId: Long) {
        updateExercises(RoutineEditing.linkWithNext(getByDay(day), exerciseId))
    }

    @Transaction
    open suspend fun unlink(day: Int, exerciseId: Long) {
        updateExercises(RoutineEditing.unlink(getByDay(day), exerciseId))
    }
}

/** Alta, cambio y baja de programas. */
@Dao
abstract class ProgramDao {

    @Query("SELECT * FROM programs ORDER BY id")
    abstract fun observePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE isActive = 1 LIMIT 1")
    abstract fun observeActive(): Flow<ProgramEntity?>

    @Query("SELECT COUNT(*) FROM programs")
    abstract suspend fun count(): Int

    @Query("SELECT name FROM programs")
    abstract suspend fun names(): List<String>

    @Query("SELECT id FROM programs WHERE isActive = 1 LIMIT 1")
    abstract suspend fun activeId(): Long?

    @Query("SELECT id FROM programs ORDER BY id LIMIT 1")
    abstract suspend fun firstId(): Long?

    @Query("SELECT imageUri FROM exercises WHERE programId = :programId AND imageUri IS NOT NULL")
    abstract suspend fun imagesOf(programId: Long): List<String>

    @Insert
    abstract suspend fun insertProgram(program: ProgramEntity): Long

    @Insert
    abstract suspend fun insertDayPlans(plans: List<DayPlanEntity>)

    @Insert
    abstract suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("UPDATE programs SET isActive = 0")
    abstract suspend fun deactivateAll()

    @Query("UPDATE programs SET isActive = 1 WHERE id = :id")
    abstract suspend fun activateRow(id: Long)

    @Query("UPDATE programs SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM programs WHERE id = :id")
    abstract suspend fun deleteRow(id: Long)

    @Transaction
    open suspend fun activate(id: Long) {
        deactivateAll()
        activateRow(id)
    }

    /** Crea el programa con sus 7 días y sus ejercicios en una sola transacción. */
    @Transaction
    open suspend fun createProgram(
        program: ProgramEntity,
        dayTitles: List<String>,
        exercises: List<ExerciseEntity>,
        activate: Boolean,
    ): Long {
        if (activate) deactivateAll()
        val id = insertProgram(program.copy(isActive = activate))
        insertDayPlans(dayTitles.mapIndexed { index, title -> DayPlanEntity(id, index + 1, title) })
        insertExercises(exercises.map { it.copy(programId = id) })
        return id
    }

    /** Borra el programa (y en cascada sus días, ejercicios y progreso). Devuelve sus imágenes. */
    @Transaction
    open suspend fun deleteProgram(id: Long): List<String> {
        val images = imagesOf(id)
        val wasActive = activeId() == id
        deleteRow(id)
        if (wasActive) firstId()?.let { activateRow(it) }
        return images
    }

    @Transaction
    open suspend fun ensureActive() {
        if (activeId() == null) firstId()?.let { activateRow(it) }
    }
}

/** Un ejercicio y su tope de series, para completar rondas de superserie. */
data class SetTarget(val exerciseId: Long, val maxSets: Int)

/** Resumen de un día para el historial. */
data class DaySummary(
    val plannedSets: Int,
    val doneSets: Int,
    val plannedExercises: Int,
    val doneExercises: Int,
)

@Dao
abstract class ProgressDao {

    @Query("SELECT * FROM set_progress WHERE date = :date")
    abstract fun observeByDate(date: String): Flow<List<SetProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(progress: SetProgressEntity)

    @Query(
        "UPDATE set_progress SET completedSets = completedSets + 1 " +
                "WHERE exerciseId = :exerciseId AND date = :date AND completedSets < :maxSets"
    )
    abstract suspend fun incrementCapped(exerciseId: Long, date: String, maxSets: Int): Int

    @Query(
        "UPDATE set_progress SET completedSets = completedSets - 1 " +
                "WHERE exerciseId = :exerciseId AND date = :date AND completedSets > 0"
    )
    abstract suspend fun decrementRow(exerciseId: Long, date: String): Int

    // ---- Resumen diario (historial) ----

    @Query("SELECT programId FROM exercises WHERE id = :exerciseId")
    abstract suspend fun programIdOf(exerciseId: Long): Long?

    @Query(
        "SELECT COALESCE(SUM(e.sets), 0) AS plannedSets, " +
                "COALESCE(SUM(MIN(COALESCE(sp.completedSets, 0), e.sets)), 0) AS doneSets, " +
                "COUNT(e.id) AS plannedExercises, " +
                "COALESCE(SUM(CASE WHEN COALESCE(sp.completedSets, 0) >= e.sets THEN 1 ELSE 0 END), 0) AS doneExercises " +
                "FROM exercises e " +
                "LEFT JOIN set_progress sp ON sp.exerciseId = e.id AND sp.date = :date " +
                "WHERE e.programId = :programId AND e.dayOfWeek = :dayOfWeek"
    )
    abstract suspend fun summarize(programId: Long, dayOfWeek: Int, date: String): DaySummary

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertLog(log: WorkoutLogEntity)

    @Query("DELETE FROM workout_log WHERE date = :date AND programId = :programId")
    abstract suspend fun deleteLog(date: String, programId: Long)

    /** Recalcula la foto del día del programa al que pertenece el ejercicio. */
    @Transaction
    open suspend fun refreshLog(exerciseId: Long, date: String) {
        val programId = programIdOf(exerciseId) ?: return
        val dayOfWeek = LocalDate.parse(date).dayOfWeek.value
        val summary = summarize(programId, dayOfWeek, date)
        if (summary.doneSets > 0) {
            upsertLog(
                WorkoutLogEntity(
                    date = date,
                    programId = programId,
                    plannedSets = summary.plannedSets,
                    doneSets = summary.doneSets,
                    plannedExercises = summary.plannedExercises,
                    doneExercises = summary.doneExercises,
                )
            )
        } else {
            deleteLog(date, programId)
        }
    }

    /** Suma una serie sin pasar del tope. Devuelve true si realmente sumó. */
    @Transaction
    open suspend fun increment(exerciseId: Long, date: String, maxSets: Int): Boolean {
        insertIfAbsent(SetProgressEntity(exerciseId, date, 0))
        val incremented = incrementCapped(exerciseId, date, maxSets) > 0
        refreshLog(exerciseId, date)
        return incremented
    }

    /** Suma una ronda completa (varios ejercicios a la vez) en una sola transacción. */
    @Transaction
    open suspend fun incrementMany(targets: List<SetTarget>, date: String): Boolean {
        var anyIncremented = false
        for (target in targets) {
            insertIfAbsent(SetProgressEntity(target.exerciseId, date, 0))
            if (incrementCapped(target.exerciseId, date, target.maxSets) > 0) {
                anyIncremented = true
            }
        }
        targets.firstOrNull()?.let { refreshLog(it.exerciseId, date) }
        return anyIncremented
    }

    @Transaction
    open suspend fun decrement(exerciseId: Long, date: String): Int {
        val rows = decrementRow(exerciseId, date)
        refreshLog(exerciseId, date)
        return rows
    }
}