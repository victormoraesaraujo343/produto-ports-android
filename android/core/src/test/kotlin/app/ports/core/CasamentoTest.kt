package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CasamentoTest {
    @Test fun `normaliza tirando acento, pontuacao e caixa`() {
        assertEquals("pokemon gold", Casamento.normalizar("Pokémon  Gold"))
        assertEquals("banjo kazooie", Casamento.normalizar("Banjo-Kazooie"))
        assertEquals("sonic 3 knuckles", Casamento.normalizar("Sonic 3 & Knuckles"))
    }

    @Test fun `pokemon gold silver crystal aceita o Gold Version pela primeira parte`() {
        assertNotNull(Casamento.pontuar("Pokemon Gold / Silver / Crystal", "Pokémon Gold Version"))
        assertNotNull(Casamento.pontuar("Pokemon Gold", "Pokémon Gold Version"))
    }

    @Test fun `nomes exatos dao nota maxima`() {
        assertEquals(1.0, Casamento.pontuar("Banjo-Kazooie", "Banjo-Kazooie")!!, 0.0)
        assertEquals(1.0, Casamento.pontuar("Sonic 3 & Knuckles", "Sonic 3 & Knuckles")!!, 0.0)
        assertEquals(1.0, Casamento.pontuar("Pokemon Gold", "Pokémon Gold")!!, 0.0)
    }

    @Test fun `nome curto contido no oficial e aceito (Real Steel)`() {
        assertNotNull(Casamento.pontuar("Real Steel", "Real Steel World Robot Boxing"))
    }

    @Test fun `parecido sem conter o outro passa so com Jaccard de 0,6`() {
        // "Star Fox Assault" não é sequência de palavras de "Star Fox Adventures Assault", mas 3 de 4 palavras batem
        assertNotNull(Casamento.pontuar("Star Fox Assault", "Star Fox Adventures Assault"))
        assertNull(Casamento.pontuar("Zelda Ocarina of Time", "Zelda Majora Mask"))
    }

    @Test fun `jogos sem relacao sao rejeitados`() {
        assertNull(Casamento.pontuar("Pokemon Gold", "Metroid Prime"))
        assertNull(Casamento.pontuar("Banjo-Kazooie", "Sonic Mania"))
        assertNull(Casamento.pontuar("Banjo-Kazooie", ""))
    }

    @Test fun `melhor escolhe a maior nota e nao o primeiro`() {
        assertEquals("Banjo-Kazooie", Casamento.melhor("Banjo-Kazooie", listOf("Banjo-Tooie", "Banjo-Kazooie")) { it })
    }

    @Test fun `melhor devolve nulo quando nenhum passa`() {
        assertNull(Casamento.melhor("Pokemon Gold", listOf("Metroid Prime", "Sonic Mania")) { it })
    }
}
