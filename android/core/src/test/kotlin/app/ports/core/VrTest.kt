package app.ports.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VrTest {
    @Test fun `titulos de quest e vr saem da lista`() {
        assertTrue(Vr.ehVr("Super Mario Galaxy (GalaxyQuest (Meta Quest 3 VR))"))
        assertTrue(Vr.ehVr("Time Crisis (Time Crisis VR)"))
        assertTrue(Vr.ehVr("Super Mario Strikers (Quest 3 Strikers)"))
    }

    @Test fun `titulos comuns ficam`() {
        assertFalse(Vr.ehVr("Super Mario 64"))
        assertFalse(Vr.ehVr("Sonic VRoom"))
        assertFalse(Vr.ehVr("Quest for Glory"))
    }
}
