package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatasTest {
    @Test fun `data iso vira dia e mes`() {
        assertEquals("03/11", Datas.curta("2024-11-03T10:00:00Z"))
        assertEquals("15/02", Datas.curta("2026-02-15"))
    }

    @Test fun `sem data ou data ruim fica nulo`() {
        assertNull(Datas.curta(null))
        assertNull(Datas.curta(""))
        assertNull(Datas.curta("ontem"))
        assertNull(Datas.curta("2024-13-01"))
    }
}
