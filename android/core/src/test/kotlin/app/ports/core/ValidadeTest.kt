package app.ports.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadeTest {
    private val agora = 1_000_000_000_000L
    private val seis = Validade.SEIS_HORAS

    @Test fun `dentro de seis horas e fresco`() = assertTrue(Validade.fresco(agora - seis + 1, agora, seis))

    @Test fun `seis horas em diante nao e fresco`() = assertFalse(Validade.fresco(agora - seis, agora, seis))

    @Test fun `sem marca nao e fresco`() = assertFalse(Validade.fresco(0L, agora, seis))

    @Test fun `relogio que voltou nao e fresco`() = assertFalse(Validade.fresco(agora + 60_000, agora, seis))
}
