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

    @Test fun `plataforma conhecida ganha nome e desconhecida volta crua`() {
        assertEquals("Nintendo 64", Plataformas.nome("n64"))
        assertEquals("Xbox 360", Plataformas.nome("xbox360"))
        assertEquals("PC", Plataformas.nome("pc"))
        assertEquals("zzz", Plataformas.nome("zzz"))
    }
}
