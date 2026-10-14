package com.darmix.darmixfit.data

/**
 * Operaciones puras sobre la lista de ejercicios de UN día (sin base de datos).
 * Una "entrada" es un ejercicio suelto o una superserie completa.
 */
object RoutineEditing {

    fun entriesOf(exercises: List<ExerciseEntity>): List<List<ExerciseEntity>> {
        val sorted = exercises.sortedWith(compareBy<ExerciseEntity>({ it.sortOrder }, { it.id }))
        val entries = mutableListOf<List<ExerciseEntity>>()
        val seenGroups = mutableSetOf<Int>()
        for (exercise in sorted) {
            val group = exercise.supersetGroup
            if (group == null) {
                entries += listOf(exercise)
            } else if (seenGroups.add(group)) {
                entries += sorted.filter { it.supersetGroup == group }
            }
        }
        return entries
    }

    /** Aplana las entradas, renumera sortOrder y disuelve superseries de un solo ejercicio. */
    private fun flatten(entries: List<List<ExerciseEntity>>): List<ExerciseEntity> =
        entries
            .flatMap { entry ->
                if (entry.size == 1 && entry.first().supersetGroup != null) {
                    listOf(entry.first().copy(supersetGroup = null))
                } else {
                    entry
                }
            }
            .mapIndexed { index, exercise -> exercise.copy(sortOrder = index) }

    fun normalize(exercises: List<ExerciseEntity>): List<ExerciseEntity> =
        flatten(entriesOf(exercises))

    fun move(exercises: List<ExerciseEntity>, exerciseId: Long, up: Boolean): List<ExerciseEntity> {
        val entries = entriesOf(exercises).toMutableList()
        val from = entries.indexOfFirst { entry -> entry.any { it.id == exerciseId } }
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to !in entries.indices) return normalize(exercises)
        val moved = entries.removeAt(from)
        entries.add(to, moved)
        return flatten(entries)
    }

    fun linkWithNext(exercises: List<ExerciseEntity>, exerciseId: Long): List<ExerciseEntity> {
        val entries = entriesOf(exercises)
        val index = entries.indexOfFirst { entry -> entry.any { it.id == exerciseId } }
        if (index < 0 || index + 1 >= entries.size) return normalize(exercises)

        val current = entries[index]
        val next = entries[index + 1]
        val group = current.first().supersetGroup
            ?: next.first().supersetGroup
            ?: ((exercises.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
        val sets = current.first().sets

        val merged = (current + next).map { it.copy(supersetGroup = group, sets = sets) }
        val result = entries.toMutableList()
        result[index] = merged
        result.removeAt(index + 1)
        return flatten(result)
    }

    fun unlink(exercises: List<ExerciseEntity>, exerciseId: Long): List<ExerciseEntity> {
        val entries = entriesOf(exercises).map { entry ->
            if (entry.any { it.id == exerciseId }) {
                entry.map { it.copy(supersetGroup = null) }
            } else {
                entry
            }
        }
        return flatten(entries)
    }
}