package com.trazo.app.domain.model

import com.trazo.app.domain.model.map.RunGoal as MapRunGoal

/** Metas de distancia que el corredor puede elegir al iniciar un recorrido. */
enum class RunGoal(
    val kilometers: Int,
) {
    KM_3(3),
    KM_5(5),
    KM_10(10),
}

fun RunGoal.toMapGoal(): MapRunGoal =
    when (this) {
        RunGoal.KM_3 -> MapRunGoal.THREE_KILOMETERS
        RunGoal.KM_5 -> MapRunGoal.FIVE_KILOMETERS
        RunGoal.KM_10 -> MapRunGoal.TEN_KILOMETERS
    }
