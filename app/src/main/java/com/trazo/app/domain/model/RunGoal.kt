package com.trazo.app.domain.model

/** Metas de distancia que el corredor puede elegir al iniciar un recorrido. */
enum class RunGoal(
    val kilometers: Int,
) {
    KM_3(3),
    KM_5(5),
    KM_10(10),
}
