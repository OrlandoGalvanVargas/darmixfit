package com.darmix.darmixfit.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * v1 -> v2: introduce los programas. Todo lo que había (ejercicios, imágenes, series y
 * progreso) pasa a ser el programa 1, que queda activo.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Copias de seguridad de los datos actuales.
        db.execSQL("CREATE TABLE plans_bak AS SELECT * FROM day_plans")
        db.execSQL("CREATE TABLE exercises_bak AS SELECT * FROM exercises")
        db.execSQL("CREATE TABLE progress_bak AS SELECT * FROM set_progress")

        // 2. Se eliminan las tablas viejas (primero la hija).
        db.execSQL("DROP TABLE set_progress")
        db.execSQL("DROP TABLE exercises")
        db.execSQL("DROP TABLE day_plans")

        // 3. Esquema nuevo.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `templateKey` TEXT, " +
                    "`isActive` INTEGER NOT NULL, `restSeconds` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_plans` (`programId` INTEGER NOT NULL, " +
                    "`dayOfWeek` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                    "PRIMARY KEY(`programId`, `dayOfWeek`), " +
                    "FOREIGN KEY(`programId`) REFERENCES `programs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`programId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, `sets` INTEGER NOT NULL, `reps` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                    "`imageUri` TEXT, `supersetGroup` INTEGER, " +
                    "FOREIGN KEY(`programId`) REFERENCES `programs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_programId` ON `exercises` (`programId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `set_progress` (`exerciseId` INTEGER NOT NULL, `date` TEXT NOT NULL, " +
                    "`completedSets` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`, `date`), " +
                    "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        // 4. El programa 1 es la rutina que ya tenías.
        db.execSQL(
            "INSERT INTO programs (id, name, description, templateKey, isActive, restSeconds) VALUES " +
                    "(1, 'Hipertrofia 5 días', " +
                    "'Empuje, tirón, pierna, torso y pierna + brazos de lunes a viernes. Es la rutina original de la app.', " +
                    "'hipertrofia_5d', 1, 30)"
        )
        db.execSQL("INSERT INTO day_plans (programId, dayOfWeek, title) SELECT 1, dayOfWeek, title FROM plans_bak")
        db.execSQL(
            "INSERT INTO exercises (id, programId, dayOfWeek, sortOrder, name, sets, reps, note, imageUri, supersetGroup) " +
                    "SELECT id, 1, dayOfWeek, sortOrder, name, sets, reps, note, imageUri, supersetGroup FROM exercises_bak"
        )
        db.execSQL(
            "INSERT INTO set_progress (exerciseId, date, completedSets) " +
                    "SELECT exerciseId, date, completedSets FROM progress_bak"
        )

        // 5. Limpieza.
        db.execSQL("DROP TABLE plans_bak")
        db.execSQL("DROP TABLE exercises_bak")
        db.execSQL("DROP TABLE progress_bak")
    }
}

/**
 * v2 -> v3: agrega el historial (workout_log) e intenta reconstruirlo a partir del
 * progreso que ya estaba guardado.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_log` (`date` TEXT NOT NULL, `programId` INTEGER NOT NULL, " +
                    "`plannedSets` INTEGER NOT NULL, `doneSets` INTEGER NOT NULL, " +
                    "`plannedExercises` INTEGER NOT NULL, `doneExercises` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`date`, `programId`), " +
                    "FOREIGN KEY(`programId`) REFERENCES `programs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_log_programId` ON `workout_log` (`programId`)")

        // Reconstruye el historial pasado. Si algo falla, la app abre igual con el historial vacío.
        try {
            db.execSQL(
                "INSERT INTO workout_log (date, programId, plannedSets, doneSets, plannedExercises, doneExercises) " +
                        "SELECT x.date, x.programId, " +
                        "SUM(e.sets), " +
                        "SUM(MIN(COALESCE(sp2.completedSets, 0), e.sets)), " +
                        "COUNT(e.id), " +
                        "SUM(CASE WHEN COALESCE(sp2.completedSets, 0) >= e.sets THEN 1 ELSE 0 END) " +
                        "FROM (" +
                        "SELECT DISTINCT sp.date AS date, e0.programId AS programId " +
                        "FROM set_progress sp JOIN exercises e0 ON e0.id = sp.exerciseId " +
                        "WHERE sp.completedSets > 0" +
                        ") x " +
                        "JOIN exercises e ON e.programId = x.programId " +
                        "AND e.dayOfWeek = (CASE CAST(strftime('%w', x.date) AS INTEGER) " +
                        "WHEN 0 THEN 7 ELSE CAST(strftime('%w', x.date) AS INTEGER) END) " +
                        "LEFT JOIN set_progress sp2 ON sp2.exerciseId = e.id AND sp2.date = x.date " +
                        "GROUP BY x.date, x.programId"
            )
        } catch (e: Exception) {
            Log.w("Migration", "No se pudo reconstruir el historial anterior", e)
        }
    }
}