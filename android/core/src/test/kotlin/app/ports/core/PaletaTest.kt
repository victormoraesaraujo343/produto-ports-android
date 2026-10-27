package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletaTest {
    @Test fun `mesmo id da mesma paleta`() {
        assertEquals(Paleta.de("sm64"), Paleta.de("sm64"))
    }

    @Test fun `ids diferentes dao paletas diferentes`() {
        assertNotEquals(Paleta.de("sm64"), Paleta.de("mania"))
    }

    @Test fun `tres cores opacas e escuras`() {
        for (id in listOf("sm64", "mania", "oot", "aabrole-huntersrecomp")) {
            val cores = Paleta.de(id)
            assertEquals(3, cores.size)
            for (c in cores) {
                assertEquals(0xFF, (c shr 24) and 0xFF)
                for (canal in listOf(16, 8, 0)) assertTrue("$id: canal ${(c shr canal) and 0xFF}", ((c shr canal) and 0xFF) <= 128)
            }
        }
    }

    @Test fun `hash e estavel`() {
        assertEquals(Paleta.hash("sm64"), Paleta.hash("sm64"))
        assertTrue(Paleta.hash("sm64") >= 0)
    }
}
