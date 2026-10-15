package com.darmix.darmixfit.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

data class ExerciseNameRow(val id: Long, val name: String)

@Dao
interface CatalogDao {

    @Query(
        "SELECT e.id, e.name FROM exercises e " +
                "INNER JOIN programs p ON p.id = e.programId " +
                "WHERE e.imageUri IS NULL AND p.templateKey IS NOT NULL AND p.templateKey != 'blank'"
    )
    suspend fun templateExercisesWithoutImage(): List<ExerciseNameRow>

    @Query("UPDATE exercises SET imageUri = :image WHERE id = :id AND imageUri IS NULL")
    suspend fun assignImage(id: Long, image: String)

    @Query(
        "UPDATE exercises SET imageUri = :image WHERE imageUri IS NULL AND name = :name AND dayOfWeek = :day " +
                "AND programId IN (SELECT id FROM programs WHERE isActive = 1)"
    )
    suspend fun assignByName(day: Int, name: String, image: String)

    @Transaction
    suspend fun assignImages(assignments: Map<Long, String>) {
        assignments.forEach { (id, image) -> assignImage(id, image) }
    }
}
