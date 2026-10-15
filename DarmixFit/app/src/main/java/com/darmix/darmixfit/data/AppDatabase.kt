package com.darmix.darmixfit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProgramEntity::class,
        DayPlanEntity::class,
        ExerciseEntity::class,
        SetProgressEntity::class,
        WorkoutLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun progressDao(): ProgressDao
    abstract fun programDao(): ProgramDao
    abstract fun historyDao(): HistoryDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "darmixfit.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
