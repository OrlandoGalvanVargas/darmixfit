package com.darmix.darmixfit.data

data class TemplateExercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val note: String = "",
    /** Mismo número dentro de un día = superserie o circuito. */
    val group: Int? = null,
)

data class TemplateDay(
    val title: String,
    val exercises: List<TemplateExercise> = emptyList(),
)

data class ProgramTemplate(
    val key: String,
    val name: String,
    val level: String,
    val restSeconds: Int,
    val summary: String,
    /** Siempre 7 elementos: lunes a domingo. */
    val days: List<TemplateDay>,
) {
    val trainingDays: Int get() = days.count { it.exercises.isNotEmpty() }
}

object ProgramTemplates {

    private fun ex(name: String, sets: Int, reps: String, note: String = "") =
        TemplateExercise(name, sets, reps, note)

    private fun ss(group: Int, name: String, sets: Int, reps: String, note: String = "") =
        TemplateExercise(name, sets, reps, note, group)

    private fun day(title: String, vararg items: TemplateExercise) =
        TemplateDay(title, items.toList())

    private val OFF = TemplateDay("Descanso")
    private val ACTIVE_REST = TemplateDay("Descanso activo o recuperación")

    private fun week(
        mon: TemplateDay = OFF,
        tue: TemplateDay = OFF,
        wed: TemplateDay = OFF,
        thu: TemplateDay = OFF,
        fri: TemplateDay = OFF,
        sat: TemplateDay = OFF,
        sun: TemplateDay = OFF,
    ) = listOf(mon, tue, wed, thu, fri, sat, sun)

    // ---------------------------------------------------------------- Hipertrofia 5 días
    private val hipertrofia5d = ProgramTemplate(
        key = "hipertrofia_5d",
        name = "Hipertrofia 5 días",
        level = "Intermedio",
        restSeconds = 30,
        summary = "Empuje, tirón, pierna, torso y pierna + brazos de lunes a viernes. " +
                "Cada grupo muscular se entrena 2 veces por semana. Es la rutina original de la app.",
        days = week(
            mon = day(
                "Empuje (Pecho, Hombro y Tríceps)",
                ex("Press de banca o suelo (Floor Press) con mancuernas", 3, "8-12 reps",
                    "Codos a unos 45° del torso. Baja con control hasta que los codos toquen el suelo."),
                ex("Lagartijas / Flexiones (Push-ups)", 3, "Al fallo",
                    "Cuerpo en línea recta y pecho cerca del suelo."),
                ex("Press militar de pie con mancuernas", 3, "10-12 reps",
                    "Abdomen firme; evita arquear la espalda baja."),
                ex("Elevaciones laterales con mancuernas", 3, "12-15 reps",
                    "Ajustable a 4 series. Sube hasta la altura del hombro con los codos ligeramente flexionados."),
                ex("Copa / Extensiones de tríceps tras nuca con disco o mancuerna", 3, "10-12 reps",
                    "Codos cerrados y quietos; solo se mueve el antebrazo."),
            ),
            tue = day(
                "Tirón (Espalda, Bíceps y Abdomen)",
                ex("Remo con mancuernas a dos manos (o unilateral)", 3, "8-12 reps",
                    "Espalda recta; tira llevando el codo hacia la cadera."),
                ex("Remo con disco / mancuerna al pecho", 3, "10-12 reps",
                    "Movimiento controlado, sin balancear el torso."),
                ex("Pull-over con mancuerna recostado", 3, "10-12 reps",
                    "Brazos casi rectos; baja detrás de la cabeza hasta sentir el estiramiento."),
                ex("Curl de bíceps alterno o con disco", 3, "10-12 reps",
                    "Sin balancear el cuerpo; codos pegados al torso."),
                ex("Curl martillo con mancuernas", 3, "10-12 reps",
                    "Palmas enfrentadas; sube sin mover los codos."),
            ),
            wed = day(
                "Pierna completa",
                ex("Sentadilla Copa (Goblet Squat) con mancuerna o disco", 4, "10-12 reps",
                    "Peso pegado al pecho; las rodillas siguen la línea de los pies."),
                ex("Peso muerto rumano con mancuernas", 4, "10-12 reps",
                    "Cadera hacia atrás, espalda neutra y rodillas ligeramente flexionadas."),
                ex("Desplantes / Zancadas con mancuernas", 3, "10-12 reps por pierna",
                    "Torso erguido; la rodilla delantera sobre el pie."),
                ex("Elevación de talones de pie con mancuernas", 4, "15-20 reps",
                    "Sube lo más alto posible y pausa un segundo arriba."),
            ),
            thu = day(
                "Torso (Pecho + Espalda + Hombro)",
                ex("Flexiones con pies elevados", 3, "10-12 reps",
                    "Cuerpo en línea recta; pies sobre una silla o banco."),
                ex("Remo con mancuerna apoyado en silla o banco", 3, "10-12 reps",
                    "Una mano apoyada y la espalda paralela al suelo."),
                ex("Press de pecho con mancuernas (agarre neutro)", 3, "10-12 reps",
                    "Palmas enfrentadas y codos cerca del cuerpo."),
                ex("Pájaros / Vuelos posteriores para hombro", 3, "12-15 reps",
                    "Torso inclinado; abre los brazos sin usar impulso."),
                ex("Elevaciones laterales de hombro", 3, "12-15 reps",
                    "Sube hasta la altura del hombro con control."),
            ),
            fri = day(
                "Pierna + Brazos (Superfoco Volumen)",
                ex("Sentadilla Búlgara con mancuernas", 3, "8-10 reps por pierna",
                    "Pie trasero apoyado en una silla; baja con el torso ligeramente inclinado."),
                ex("Puente de glúteo / Hip Thrust con disco o mancuerna", 3, "12-15 reps",
                    "Aprieta los glúteos arriba y pausa un segundo."),
                ss(1, "A1: Curl de bíceps", 3, "12 reps",
                    "Superserie con A2: sin descanso entre ambos ejercicios."),
                ss(1, "A2: Extensiones de tríceps sobre la cabeza", 3, "12 reps",
                    "Descansa solo al terminar A1 + A2."),
            ),
            sat = ACTIVE_REST,
            sun = ACTIVE_REST,
        ),
    )

    // ---------------------------------------------------------------- Cuerpo completo 3 días
    private val fullBody3d = ProgramTemplate(
        key = "fullbody_3d",
        name = "Cuerpo completo 3 días",
        level = "Principiante a intermedio",
        restSeconds = 60,
        summary = "Tres sesiones de cuerpo completo (lunes, miércoles y viernes). Cada músculo se trabaja " +
                "3 veces por semana con unas 9 a 12 series semanales. Ideal si prefieres pocos días fijos.",
        days = week(
            mon = day(
                "Cuerpo completo A",
                ex("Sentadilla Copa (Goblet Squat) con mancuerna", 3, "10-12 reps",
                    "Peso pegado al pecho; baja hasta donde mantengas la espalda neutra."),
                ex("Press de pecho con mancuernas (banco o suelo)", 3, "8-12 reps",
                    "Codos a unos 45° del torso."),
                ex("Remo con mancuerna a una mano", 3, "8-12 reps por lado",
                    "Apoya una mano en una silla; tira del codo hacia la cadera."),
                ex("Press militar con mancuernas", 3, "8-12 reps",
                    "Abdomen firme; sin arquear la espalda baja."),
                ss(1, "Curl de bíceps alterno", 2, "12 reps",
                    "Superserie con el siguiente: sin descanso entre ambos."),
                ss(1, "Extensión de tríceps sobre la cabeza", 2, "12 reps",
                    "Descansa solo al terminar la ronda."),
                ex("Plancha", 3, "30-45 segundos", "Cuerpo en línea recta; aprieta abdomen y glúteos."),
            ),
            wed = day(
                "Cuerpo completo B",
                ex("Peso muerto rumano con mancuernas", 3, "10-12 reps",
                    "Cadera hacia atrás, espalda neutra y rodillas ligeramente flexionadas."),
                ex("Flexiones (Push-ups)", 3, "8-15 reps",
                    "Si te resultan fáciles, apoya los pies en una silla."),
                ex("Remo con mancuernas a dos manos", 3, "10-12 reps",
                    "Torso inclinado con la espalda recta; sin balancear."),
                ex("Zancadas con mancuernas", 3, "10 reps por pierna",
                    "Torso erguido; la rodilla delantera sobre el pie."),
                ex("Elevaciones laterales con mancuernas", 3, "12-15 reps",
                    "Sube hasta la altura del hombro con control."),
                ex("Elevación de talones de pie con mancuernas", 3, "15-20 reps",
                    "Sube lo más alto posible y pausa un segundo arriba."),
            ),
            fri = day(
                "Cuerpo completo C",
                ex("Sentadilla Búlgara con mancuernas", 3, "8-12 reps por pierna",
                    "Pie trasero apoyado en una silla; baja con el torso ligeramente inclinado."),
                ex("Press de pecho con mancuernas (agarre neutro)", 3, "10-12 reps",
                    "Palmas enfrentadas y codos cerca del cuerpo."),
                ex("Pull-over con mancuerna recostado", 3, "10-12 reps",
                    "Brazos casi rectos; baja detrás de la cabeza hasta sentir el estiramiento."),
                ex("Puente de glúteo / Hip Thrust con mancuerna", 3, "12-15 reps",
                    "Aprieta los glúteos arriba y pausa un segundo."),
                ex("Pájaros / Vuelos posteriores para hombro", 3, "12-15 reps",
                    "Torso inclinado; abre los brazos sin usar impulso."),
                ss(1, "Curl martillo con mancuernas", 2, "12 reps",
                    "Superserie con el siguiente: sin descanso entre ambos."),
                ss(1, "Copa / Extensión de tríceps tras nuca", 2, "12 reps",
                    "Codos cerrados; descansa al terminar la ronda."),
            ),
        ),
    )

    // ---------------------------------------------------------------- Torso / Pierna 4 días
    private val upperLower4d = ProgramTemplate(
        key = "upper_lower_4d",
        name = "Torso / Pierna 4 días",
        level = "Intermedio",
        restSeconds = 90,
        summary = "Dos días de torso y dos de pierna (lunes, martes, jueves y viernes). Cada grupo se " +
                "entrena 2 veces por semana: un día más pesado y otro de más volumen.",
        days = week(
            mon = day(
                "Torso A (pesado)",
                ex("Press de pecho con mancuernas (banco o suelo)", 4, "8-10 reps",
                    "Usa una carga con la que te queden 1 o 2 repeticiones en reserva."),
                ex("Remo con mancuerna a una mano", 4, "8-10 reps por lado",
                    "Aprieta la espalda arriba y baja lento."),
                ex("Press militar con mancuernas", 3, "8-10 reps",
                    "Abdomen firme; sin arquear la espalda."),
                ex("Pull-over con mancuerna recostado", 3, "10-12 reps",
                    "Brazos casi rectos; siente el estiramiento de la espalda."),
                ex("Elevaciones laterales con mancuernas", 3, "12-15 reps",
                    "Sube hasta la altura del hombro con control."),
                ss(1, "Curl de bíceps", 2, "10-12 reps", "Superserie con el siguiente."),
                ss(1, "Extensión de tríceps sobre la cabeza", 2, "10-12 reps",
                    "Descansa al terminar la ronda."),
            ),
            tue = day(
                "Pierna A (pesado)",
                ex("Sentadilla Copa (Goblet Squat) con mancuerna", 4, "8-10 reps",
                    "Peso pegado al pecho; las rodillas siguen la línea de los pies."),
                ex("Peso muerto rumano con mancuernas", 4, "8-10 reps",
                    "Cadera hacia atrás, espalda neutra."),
                ex("Sentadilla sumo con mancuerna", 3, "10-12 reps",
                    "Pies abiertos con las puntas hacia afuera; rodillas en línea con los pies."),
                ex("Puente de glúteo con mancuerna", 3, "10-12 reps",
                    "Aprieta los glúteos arriba y pausa un segundo."),
                ex("Elevación de talones de pie con mancuernas", 4, "12-15 reps",
                    "Sube lo más alto posible y pausa un segundo arriba."),
                ex("Plancha", 3, "30-45 segundos", "Cuerpo en línea recta; aprieta abdomen y glúteos."),
            ),
            thu = day(
                "Torso B (volumen)",
                ex("Flexiones con pies elevados", 4, "10-15 reps",
                    "Cuerpo en línea recta; pies sobre una silla o banco."),
                ex("Remo con mancuernas a dos manos", 4, "10-12 reps",
                    "Torso inclinado con la espalda recta; sin balancear."),
                ex("Press de pecho con mancuernas (agarre neutro)", 3, "10-12 reps",
                    "Palmas enfrentadas y codos cerca del cuerpo."),
                ex("Pájaros / Vuelos posteriores para hombro", 3, "12-20 reps",
                    "Torso inclinado; abre los brazos sin usar impulso."),
                ex("Press Arnold con mancuernas", 3, "10-12 reps",
                    "Gira las palmas mientras subes; movimiento fluido y controlado."),
                ex("Curl martillo con mancuernas", 3, "10-12 reps",
                    "Palmas enfrentadas; sube sin mover los codos."),
                ex("Copa / Extensión de tríceps tras nuca", 3, "12 reps",
                    "Codos cerrados y quietos; solo se mueve el antebrazo."),
            ),
            fri = day(
                "Pierna B (volumen)",
                ex("Sentadilla Búlgara con mancuernas", 4, "8-12 reps por pierna",
                    "Pie trasero apoyado en una silla; baja con el torso ligeramente inclinado."),
                ex("Peso muerto rumano a una pierna", 3, "8-12 reps por pierna",
                    "Apóyate en una pared o silla si pierdes el equilibrio."),
                ex("Subidas a la silla (Step-up) con mancuernas", 3, "10 reps por pierna",
                    "Usa una silla estable; empuja con la pierna de arriba."),
                ex("Puente de glúteo / Hip Thrust con disco o mancuerna", 4, "10-15 reps",
                    "Aprieta los glúteos arriba y pausa un segundo."),
                ex("Elevación de talones sentado (mancuerna sobre las rodillas)", 4, "15-20 reps",
                    "Recorrido completo, sin rebotar."),
                ex("Crunch con mancuerna", 3, "15 reps", "Sube el torso con el abdomen, sin jalar el cuello."),
            ),
        ),
    )

    // ---------------------------------------------------------------- Express 2 días
    private val express2d = ProgramTemplate(
        key = "express_2d",
        name = "Express 2 días",
        level = "Cualquiera",
        restSeconds = 45,
        summary = "Para semanas apretadas: dos sesiones cortas (unos 35 minutos) de cuerpo completo, con " +
                "superseries para ahorrar tiempo. Es una dosis mínima efectiva para mantener y seguir progresando.",
        days = week(
            mon = day(
                "Cuerpo completo A",
                ex("Sentadilla Copa (Goblet Squat) con mancuerna", 3, "8-12 reps",
                    "Peso pegado al pecho; baja con control."),
                ex("Press de pecho con mancuernas (banco o suelo)", 3, "8-12 reps",
                    "Codos a unos 45° del torso."),
                ex("Remo con mancuerna a una mano", 3, "8-12 reps por lado",
                    "Tira del codo hacia la cadera; sin impulso."),
                ss(1, "Peso muerto rumano con mancuernas", 3, "10 reps",
                    "Superserie con el siguiente: sin descanso entre ambos."),
                ss(1, "Press militar de pie con mancuernas", 3, "10 reps",
                    "Descansa al terminar la ronda."),
                ex("Plancha", 2, "30-45 segundos", "Cuerpo en línea recta."),
            ),
            thu = day(
                "Cuerpo completo B",
                ex("Sentadilla Búlgara con mancuernas", 3, "8-10 reps por pierna",
                    "Pie trasero apoyado en una silla."),
                ex("Flexiones (Push-ups)", 3, "Cerca del fallo",
                    "Cuerpo en línea recta y pecho cerca del suelo."),
                ex("Remo con mancuernas a dos manos", 3, "10-12 reps",
                    "Torso inclinado con la espalda recta."),
                ss(1, "Puente de glúteo / Hip Thrust con mancuerna", 3, "12 reps",
                    "Superserie con el siguiente: sin descanso entre ambos."),
                ss(1, "Elevaciones laterales con mancuernas", 3, "12-15 reps",
                    "Descansa al terminar la ronda."),
                ss(2, "Curl de bíceps alterno", 2, "12 reps",
                    "Superserie con el siguiente: sin descanso entre ambos."),
                ss(2, "Extensión de tríceps sobre la cabeza", 2, "12 reps",
                    "Descansa al terminar la ronda."),
            ),
        ),
    )

    // ---------------------------------------------------------------- Fuerza 3 días
    private val fuerza3d = ProgramTemplate(
        key = "fuerza_3d",
        name = "Fuerza 3 días",
        level = "Intermedio",
        restSeconds = 150,
        summary = "Cargas altas y pocas repeticiones (5 a 8) con descansos largos. Prioriza movimientos " +
                "pesados y a una pierna o brazo para exigir más a las mancuernas.",
        days = week(
            mon = day(
                "Fuerza A",
                ex("Sentadilla Copa pesada (Goblet Squat)", 4, "5-8 reps",
                    "Usa la mancuerna más pesada que controles; pausa 1 segundo abajo."),
                ex("Press de pecho con mancuernas (suelo o banco)", 4, "5-8 reps",
                    "Baja en 2 o 3 segundos y sube con fuerza."),
                ex("Remo con mancuerna a una mano", 4, "6-8 reps por lado",
                    "Sin impulso; tira fuerte y baja lento."),
                ex("Press militar de pie con mancuernas", 3, "5-8 reps",
                    "Glúteos y abdomen firmes."),
                ex("Peso muerto rumano con mancuernas", 3, "6-8 reps",
                    "Espalda neutra; siente el estiramiento de los isquiotibiales."),
            ),
            wed = day(
                "Fuerza B (a una pierna)",
                ex("Sentadilla Búlgara con mancuernas", 4, "5-8 reps por pierna",
                    "Pie trasero en una silla; mancuernas pesadas a los lados."),
                ex("Flexiones con pies elevados", 4, "6-10 reps",
                    "Agrega peso (una mochila) cuando superes las 10 repeticiones."),
                ex("Remo con mancuernas a dos manos", 4, "6-8 reps",
                    "Torso inclinado con la espalda recta; sin balancear."),
                ex("Peso muerto rumano a una pierna", 3, "6-8 reps por pierna",
                    "Apóyate en una pared si pierdes el equilibrio."),
                ss(1, "Curl de bíceps", 2, "8-10 reps", "Superserie con el siguiente."),
                ss(1, "Extensión de tríceps sobre la cabeza", 2, "8-10 reps",
                    "Descansa al terminar la ronda."),
            ),
            fri = day(
                "Fuerza C",
                ex("Sentadilla frontal con dos mancuernas", 4, "5-8 reps",
                    "Mancuernas apoyadas sobre los hombros; torso erguido."),
                ex("Press de pecho con mancuernas (agarre neutro)", 4, "5-8 reps",
                    "Palmas enfrentadas y codos cerca del cuerpo."),
                ex("Remo con mancuerna apoyado en silla o banco", 4, "6-8 reps por lado",
                    "Una mano apoyada y la espalda paralela al suelo."),
                ex("Press militar sentado con mancuernas", 3, "6-8 reps",
                    "Espalda apoyada en el respaldo; sube sin arquear."),
                ex("Puente de glúteo / Hip Thrust con mancuerna pesada", 3, "8-10 reps",
                    "Aprieta los glúteos arriba y pausa un segundo."),
            ),
        ),
    )

    // ---------------------------------------------------------------- Circuitos 4 días
    private val circuitos4d = ProgramTemplate(
        key = "circuitos_4d",
        name = "Circuitos 4 días",
        level = "Cualquiera",
        restSeconds = 60,
        summary = "Circuitos de 5 ejercicios seguidos, con descanso solo al terminar cada ronda. Más " +
                "gasto energético y resistencia. Tres días de circuito y uno de cardio suave con core.",
        days = week(
            mon = day(
                "Circuito A (piernas y empuje)",
                ss(1, "Sentadilla Copa (Goblet Squat) con mancuerna", 4, "15 reps",
                    "Circuito: haz los 5 ejercicios seguidos y descansa al terminar la ronda."),
                ss(1, "Press de pecho con mancuernas en el suelo", 4, "12-15 reps",
                    "Baja con control y sube sin bloquear los codos."),
                ss(1, "Remo con mancuerna a una mano", 4, "12 reps por lado",
                    "Apoya una mano en una silla; espalda recta."),
                ss(1, "Zancadas alternas con mancuernas", 4, "10 reps por pierna",
                    "Torso erguido; la rodilla delantera sobre el pie."),
                ss(1, "Flexiones (Push-ups)", 4, "10-15 reps",
                    "Cuerpo en línea recta; apoya las rodillas si hace falta."),
            ),
            wed = day(
                "Circuito B (tirón y cadera)",
                ss(1, "Peso muerto rumano con mancuernas", 4, "12-15 reps",
                    "Circuito: haz los 5 ejercicios seguidos y descansa al terminar la ronda."),
                ss(1, "Press militar de pie con mancuernas", 4, "12 reps",
                    "Abdomen firme; sin arquear la espalda baja."),
                ss(1, "Remo con mancuernas a dos manos", 4, "12-15 reps",
                    "Torso inclinado con la espalda recta."),
                ss(1, "Sentadilla sumo con mancuerna", 4, "15 reps",
                    "Pies abiertos con las puntas hacia afuera."),
                ss(1, "Plancha", 4, "30 segundos", "Cuerpo en línea recta; aprieta abdomen y glúteos."),
            ),
            fri = day(
                "Circuito C (cuerpo completo)",
                ss(1, "Swing con mancuerna", 4, "15-20 reps",
                    "Circuito: el impulso sale de la cadera, con los brazos relajados."),
                ss(1, "Thruster con mancuernas (sentadilla + press)", 4, "12 reps",
                    "Baja a sentadilla y sube empujando las mancuernas sobre la cabeza."),
                ss(1, "Remo con mancuernas a dos manos", 4, "12 reps",
                    "Torso inclinado con la espalda recta."),
                ss(1, "Zancadas caminando con mancuernas", 4, "10 reps por pierna",
                    "Pasos largos y controlados."),
                ss(1, "Flexiones (Push-ups)", 4, "Cerca del fallo",
                    "Cuerpo en línea recta y pecho cerca del suelo."),
            ),
            sat = day(
                "Cardio suave y core",
                ex("Caminata rápida o trote suave", 1, "30 minutos",
                    "Ritmo en el que puedas hablar con frases cortas."),
                ex("Plancha", 3, "30-45 segundos", "Cuerpo en línea recta."),
                ex("Bicicleta abdominal", 3, "20 reps", "Movimiento lento; el codo busca la rodilla contraria."),
                ex("Elevación de piernas tumbado", 3, "12 reps",
                    "Baja las piernas sin despegar la espalda baja del suelo."),
            ),
        ),
    )

    // ---------------------------------------------------------------- Calistenia 4 días
    private val calistenia4d = ProgramTemplate(
        key = "calistenia_4d",
        name = "Calistenia 4 días",
        level = "Cualquiera",
        restSeconds = 60,
        summary = "Sin mancuernas: peso corporal, una silla y una mesa firme. Empuje, piernas, tirón con " +
                "core y un circuito completo.",
        days = week(
            mon = day(
                "Empuje (pecho, hombro y tríceps)",
                ex("Flexiones (Push-ups)", 4, "Cerca del fallo",
                    "Cuerpo en línea recta y pecho cerca del suelo."),
                ex("Flexiones diamante", 3, "8-12 reps",
                    "Manos juntas bajo el pecho; codos cerca del cuerpo."),
                ex("Flexiones pike (hombros)", 3, "8-12 reps",
                    "Cadera arriba en forma de V; baja la cabeza entre las manos."),
                ex("Fondos de tríceps en silla", 3, "10-15 reps",
                    "Silla estable y bien apoyada; baja hasta unos 90° en los codos."),
                ex("Flexiones con pies elevados", 3, "10-15 reps",
                    "Pies sobre una silla; cuerpo en línea recta."),
            ),
            tue = day(
                "Piernas y glúteos",
                ex("Sentadilla libre", 4, "15-20 reps", "Rodillas en línea con los pies; talones apoyados."),
                ex("Zancadas caminando", 3, "12 reps por pierna", "Torso erguido y pasos controlados."),
                ex("Sentadilla Búlgara sin peso", 3, "10-15 reps por pierna",
                    "Pie trasero apoyado en una silla."),
                ex("Puente de glúteo a una pierna", 3, "12 reps por pierna",
                    "Aprieta el glúteo arriba y mantén la cadera nivelada."),
                ex("Elevación de talones de pie", 4, "20 reps", "Sube lo más alto posible y pausa arriba."),
                ex("Sentadilla isométrica en la pared", 3, "30-45 segundos",
                    "Muslos paralelos al suelo y espalda apoyada en la pared."),
            ),
            thu = day(
                "Tirón y core",
                ex("Remo invertido bajo una mesa resistente", 4, "8-12 reps",
                    "Solo con una mesa muy firme y estable; cuerpo recto y tira el pecho hacia el borde. " +
                            "Si no tienes una segura, omite este ejercicio."),
                ex("Superman", 3, "12-15 reps", "Levanta pecho y piernas del suelo y pausa 1 segundo."),
                ex("Plancha", 3, "30-60 segundos", "Cuerpo en línea recta; aprieta abdomen y glúteos."),
                ex("Elevación de piernas tumbado", 3, "10-15 reps",
                    "Baja las piernas sin despegar la espalda baja del suelo."),
                ex("Bicicleta abdominal", 3, "20 reps", "Movimiento lento; el codo busca la rodilla contraria."),
            ),
            fri = day(
                "Circuito de cuerpo completo",
                ss(1, "Flexiones (Push-ups)", 3, "10-15 reps",
                    "Circuito: haz los 5 ejercicios seguidos y descansa al terminar la ronda."),
                ss(1, "Sentadilla libre", 3, "20 reps", "Rodillas en línea con los pies."),
                ss(1, "Remo invertido bajo una mesa resistente", 3, "10 reps",
                    "Solo con una mesa muy firme; si no tienes una segura, sustitúyelo por Superman."),
                ss(1, "Zancadas alternas", 3, "10 reps por pierna", "Torso erguido."),
                ss(1, "Plancha", 3, "40 segundos", "Cuerpo en línea recta."),
            ),
        ),
    )

    // ---------------------------------------------------------------- En blanco
    val blank = ProgramTemplate(
        key = "blank",
        name = "Programa en blanco",
        level = "—",
        restSeconds = 30,
        summary = "Un programa vacío para armarlo tú desde la pestaña Rutina.",
        days = List(7) { TemplateDay("Sin plan") },
    )

    /** Plantillas que se ofrecen en la pantalla de programas. */
    val all: List<ProgramTemplate> = listOf(
        hipertrofia5d,
        fullBody3d,
        upperLower4d,
        express2d,
        fuerza3d,
        circuitos4d,
        calistenia4d,
    )

    /** La que se instala en un teléfono nuevo. */
    val default: ProgramTemplate = hipertrofia5d
}