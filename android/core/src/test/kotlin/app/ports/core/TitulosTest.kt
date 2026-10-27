package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TitulosTest {
    @Test fun `logo tira parenteses aninhados e poe em maiusculas`() {
        assertEquals("METROID PRIME HUNTERS", Titulo.logo("Metroid Prime Hunters (HuntersRecomp (AYN Thor))"))
        assertEquals("SUPER MARIO 64", Titulo.logo("Super Mario 64 (EUA)"))
    }

    @Test fun `busca tira as palavras de recompilacao`() {
        assertEquals("Super Mario 64", Titulo.limparParaBusca("Super Mario 64 (Recomp)"))
        assertEquals("Sonic Mania", Titulo.limparParaBusca("Sonic Mania Native PC Port"))
        assertEquals("Zelda Ocarina of Time", Titulo.limparParaBusca("Zelda Ocarina of Time - Recompiled"))
    }

    @Test fun `busca nao corta palavras que so contem o termo`() {
        assertEquals("PCSX Rearmed", Titulo.limparParaBusca("PCSX Rearmed"))
    }

    @Test fun `consultas trazem a primeira parte quando ha barra e Pokemon sem acento`() {
        assertEquals(listOf("Pokemon Gold / Silver / Crystal", "Pokemon Gold"), Titulo.consultas("Pokémon Gold / Silver / Crystal (Recomp)"))
        assertEquals(listOf("Banjo-Kazooie"), Titulo.consultas("Banjo-Kazooie (Recomp)"))
    }

    @Test fun `por parte cada jogo do titulo vira uma busca e a franquia passa pra parte de uma palavra`() {
        assertEquals(listOf("Pokemon Gold", "Pokemon Silver", "Pokemon Crystal"), Titulo.porParte("Pokémon Gold / Silver / Crystal (Recomp)"))
        assertEquals(listOf("Banjo-Kazooie"), Titulo.porParte("Banjo-Kazooie (Recomp)"))
    }

    @Test fun `plataforma conhecida ganha nome e desconhecida volta crua`() {
        assertEquals("Nintendo 64", Plataformas.nome("n64"))
        assertEquals("Xbox 360", Plataformas.nome("xbox360"))
        assertEquals("PC", Plataformas.nome("pc"))
        assertEquals("zzz", Plataformas.nome("zzz"))
    }
}
