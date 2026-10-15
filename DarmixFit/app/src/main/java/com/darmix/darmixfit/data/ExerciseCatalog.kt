package com.darmix.darmixfit.data

import java.text.Normalizer

object ExerciseCatalog {

    private val rules: List<Pair<String, List<String>>> = listOf(
        "flexiones_pies_elevados" to listOf("flexiones", "pies elevados"),
        "flexiones_diamante" to listOf("diamante"),
        "flexiones_pike" to listOf("pike"),
        "fondos_silla" to listOf("fondos"),
        "flexiones" to listOf("flexiones"),
        "thruster" to listOf("thruster"),
        "press_pecho_neutro" to listOf("press de pecho", "neutro"),
        "press_banca" to listOf("press de banca"),
        "press_banca" to listOf("press de pecho"),
        "press_arnold" to listOf("arnold"),
        "press_militar_sentado" to listOf("press militar sentado"),
        "press_militar" to listOf("press militar"),
        "elevaciones_laterales" to listOf("elevaciones laterales"),
        "pajaros" to listOf("pajaros"),
        "extension_triceps" to listOf("triceps"),
        "remo_invertido" to listOf("remo invertido"),
        "remo_al_pecho" to listOf("remo con disco"),
        "remo_una_mano" to listOf("remo con mancuerna a una mano"),
        "remo_una_mano" to listOf("remo con mancuerna apoyado"),
        "remo_dos_manos" to listOf("remo"),
        "pullover" to listOf("pull-over"),
        "curl_martillo" to listOf("martillo"),
        "curl_biceps" to listOf("curl"),
        "sentadilla_goblet" to listOf("goblet"),
        "sentadilla_bulgara" to listOf("bulgara"),
        "sentadilla_sumo" to listOf("sumo"),
        "sentadilla_frontal" to listOf("sentadilla frontal"),
        "sentadilla_pared" to listOf("isometrica"),
        "sentadilla_libre" to listOf("sentadilla"),
        "peso_muerto_una_pierna" to listOf("peso muerto", "una pierna"),
        "peso_muerto_rumano" to listOf("peso muerto"),
        "zancadas" to listOf("zancadas"),
        "step_up" to listOf("step-up"),
        "puente_una_pierna" to listOf("puente", "una pierna"),
        "hip_thrust" to listOf("puente"),
        "elevacion_talones_sentado" to listOf("talones sentado"),
        "elevacion_talones" to listOf("talones"),
        "plancha" to listOf("plancha"),
        "crunch" to listOf("crunch"),
        "bicicleta_abdominal" to listOf("bicicleta"),
        "elevacion_piernas" to listOf("elevacion de piernas"),
        "superman" to listOf("superman"),
        "swing" to listOf("swing"),
        "caminata" to listOf("caminata")
    )

    val slugs: List<String> = rules.map { it.first }.distinct()

    fun slugFor(exerciseName: String): String? {
        val normalized = normalize(exerciseName)
        return rules.firstOrNull { (_, keywords) -> keywords.all { it in normalized } }?.first
    }

    fun imageRefFor(exerciseName: String): String? =
        slugFor(exerciseName)?.let(ExerciseImageStore::bundledRef)

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
