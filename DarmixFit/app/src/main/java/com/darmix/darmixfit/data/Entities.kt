package com.darmix.darmixfit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    /** Plantilla de origen (para saber de dónde salió el programa). */
    val templateKey: String? = null,
    val isActive: Boolean = false,
    /** Descanso sugerido entre series, en segundos. */
    val restSeconds: Int = 30,
)

/** dayOfWeek: 1 = lunes ... 7 = domingo (igual que java.time.DayOfWeek.value). */
@Entity(
    tableName = "day_plans",
    primaryKeys = ["programId", "dayOfWeek"],
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class DayPlanEntity(
    val programId: Long,
    val dayOfWeek: Int,
    val title: String,
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("programId")],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val dayOfWeek: Int,
    val sortOrder: Int,
    val name: String,
    val sets: Int,
    val reps: String,
    val note: String = "",
    /** Nombre del archivo de imagen guardado en la app (no es una URI real). */
    val imageUri: String? = null,
    /** Los ejercicios del mismo día con el mismo número forman una superserie o circuito. */
    val supersetGroup: Int? = null,
)

/** Series completadas de un ejercicio en una fecha concreta (formato yyyy-MM-dd). */
@Entity(
    tableName = "set_progress",
    primaryKeys = ["exerciseId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class SetProgressEntity(
    val exerciseId: Long,
    val date: String,
    val completedSets: Int,
)

/** Foto del día: qué tanto se completó el plan de un programa en una fecha. */
@Entity(
    tableName = "workout_log",
    primaryKeys = ["date", "programId"],
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("programId")],
)
data class WorkoutLogEntity(
    val date: String,
    val programId: Long,
    val plannedSets: Int,
    val doneSets: Int,
    val plannedExercises: Int,
    val doneExercises: Int,
)