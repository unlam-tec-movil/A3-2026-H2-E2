package com.trazo.app.ui.theme

import androidx.compose.ui.unit.dp

/** Espaciado en grilla de 4 dp. */
object Spacing {
    val xxs = 2.dp // valor y unidad de una métrica
    val xs = 4.dp // ícono y texto en chip/badge
    val sm = 8.dp // entre chips
    val md = 12.dp // entre cards del Historial
    val lg = 16.dp // margen lateral de pantalla, padding de cards
    val xl = 24.dp // padding de modales, entre secciones
    val xxl = 32.dp // bloques de «Guardar recorrido»
    val xxxl = 48.dp // aire de ilustraciones de estado vacío
}

object Sizes {
    val minTouchTarget = 48.dp
    val runControl = 56.dp // Pausar / Reanudar / Finalizar: se tocan corriendo
    val buttonHeight = 48.dp
    val traceLiveWidth = 6.dp
    val traceSavedWidth = 4.dp
    val gpsIndicatorHeight = 28.dp // píldora del indicador de GPS (referencia §6.11)
}

/** Niveles de elevación de Material 3 (referencia §5.4). */
object Elevation {
    val level1 = 1.dp // panel del temporizador sobre el mapa
    val level2 = 3.dp // botón «Centrar», opción «Iniciar un recorrido»
    val level3 = 6.dp // FAB, modales, alerta de frenada
}
