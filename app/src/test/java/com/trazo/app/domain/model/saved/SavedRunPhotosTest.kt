package com.trazo.app.domain.model.saved

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedRunPhotosTest {
    @Test
    fun `un recorrido admite como maximo 2 fotos`() {
        assertEquals(2, SavedRun.MAX_PHOTOS)
    }
}
