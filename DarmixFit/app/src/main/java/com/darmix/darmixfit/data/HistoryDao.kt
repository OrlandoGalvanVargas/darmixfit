package com.darmix.darmixfit.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Foto del día junto con el nombre del programa. */
data class DayLogRow(
    @Embedded val log: WorkoutLogEntity,
    val programName: String,
)

/** Un ejercicio con progreso en una fecha. */
data class HistoryExerciseRow(
    val name: String,
    val sets: Int,
    val completedSets: Int,
    val programId: Long,
)

/** Rutina completa: todas las series planeadas están hechas. */
val WorkoutLogEntity.isComplete: Boolean
    get() = plannedSets > 0 && doneSets >= plannedSets

@Dao
abstract class HistoryDao {

    @Query("SELECT * FROM workout_log WHERE date >= :from AND date <= :to")
    abstract fun observeLogs(from: String, to: String): Flow<List<WorkoutLogEntity>>

    @Query(
        "SELECT l.*, p.name AS programName FROM workout_log l " +
                "JOIN programs p ON p.id = l.programId WHERE l.date = :date ORDER BY p.id"
    )
    abstract fun observeDayLogs(date: String): Flow<List<DayLogRow>>

    @Query(
        "SELECT e.name AS name, e.sets AS sets, sp.completedSets AS completedSets, e.programId AS programId " +
                "FROM set_progress sp JOIN exercises e ON e.id = sp.exerciseId " +
                "WHERE sp.date = :date AND sp.completedSets > 0 ORDER BY e.programId, e.sortOrder"
    )
    abstract fun observeDayExercises(date: String): Flow<List<HistoryExerciseRow>>

    @Query(
        "SELECT DISTINCT date FROM workout_log " +
                "WHERE plannedSets > 0 AND doneSets >= plannedSets ORDER BY date"
    )
    abstract fun observeCompleteDates(): Flow<List<String>>
}