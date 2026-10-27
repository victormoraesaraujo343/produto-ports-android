package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsolesTest {
    private fun porta(id: String, plataforma: String, estado: Estado = Estado.DISPONIVEL) =
        PortNaTela(Port(id, id, id, "", "", plataforma, "", "", "", "", null, null, null), estado, null)

    @Test fun `plataforma vazia ou other vai pra Outros`() {
        assertEquals("Outros", Consoles.nomeDe(""))
        assertEquals("Outros", Consoles.nomeDe("   "))
        assertEquals("Outros", Consoles.nomeDe("other"))
        assertEquals("Nintendo 64", Consoles.nomeDe(" Nintendo 64 "))
    }

    @Test fun `agrupa por contagem desc e depois por nome, com Outros por ultimo`() {
        val ports = listOf(
            porta("a", "PlayStation"), porta("b", "Nintendo 64"), porta("c", "Dreamcast"),
            porta("d", "Nintendo 64"), porta("e", ""), porta("f", "Dreamcast"), porta("g", "Nintendo 64"),
            porta("h", "other"), porta("i", "other"), porta("j", "other"), porta("k", "other"),
        )
        assertEquals(
            listOf("Nintendo 64" to 3, "Dreamcast" to 2, "PlayStation" to 1, "Outros" to 5),
            Consoles.agrupar(ports),
        )
    }

    @Test fun `PC aparece como console normal`() {
        val ports = listOf(porta("a", "PC"), porta("b", "PC"), porta("c", "Dreamcast"))
        assertEquals(listOf("PC" to 2, "Dreamcast" to 1), Consoles.agrupar(ports))
        assertEquals(listOf("a", "b"), Consoles.filtrar(ports, "PC").map { it.port.id })
    }

    @Test fun `filtra por console e nulo mostra tudo`() {
        val ports = listOf(porta("a", "Nintendo 64"), porta("b", "Dreamcast"), porta("c", ""))
        assertEquals(3, Consoles.filtrar(ports, null).size)
        assertEquals(listOf("a"), Consoles.filtrar(ports, "Nintendo 64").map { it.port.id })
        assertEquals(listOf("c"), Consoles.filtrar(ports, "Outros").map { it.port.id })
    }

    @Test fun `contagem das abas reflete o console escolhido`() {
        val ports = listOf(
            porta("a", "Nintendo 64", Estado.PRONTO),
            porta("b", "Nintendo 64", Estado.DISPONIVEL),
            porta("c", "Dreamcast", Estado.PRONTO),
        )
        val n64 = Consoles.filtrar(ports, "Nintendo 64")
        assertEquals(1, n64.count(Filtro.INSTALADOS.aceita))
        assertEquals(1, n64.count(Filtro.DISPONIVEIS.aceita))
        assertEquals(2, Consoles.filtrar(ports, null).count(Filtro.INSTALADOS.aceita))
    }
}
