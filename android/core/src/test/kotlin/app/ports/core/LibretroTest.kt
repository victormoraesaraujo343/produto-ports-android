package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibretroTest {
    private val listagem = """
        <html><body><table>
        <tr><td><a href="?C=N;O=D">Name</a></td></tr>
        <tr><td><a href="/Nintendo%20-%20Nintendo%2064/">Parent Directory</a></td></tr>
        <tr><td><a href="Banjo-Kazooie%20%28USA%29.png">Banjo-Kazooie (USA).png</a></td></tr>
        <tr><td><a href='Banjo-Kazooie%20%28Europe%29%20%28En%2CFr%2CDe%29.png'>x</a></td></tr>
        <tr><td><a href="Banjo-Tooie%20%28Australia%29.png">Banjo-Tooie (Australia).png</a></td></tr>
        <tr><td><a href="Leia%20isto.txt">txt</a></td></tr>
        </table></body></html>
    """.trimIndent()

    @Test fun `listagem traz so os png, sem tags de html, decodificados`() {
        assertEquals(
            listOf("Banjo-Kazooie (USA)", "Banjo-Kazooie (Europe) (En,Fr,De)", "Banjo-Tooie (Australia)"),
            Libretro.nomesDaListagem(listagem),
        )
    }

    @Test fun `casamento ignora as tags de regiao e revisao`() {
        val nomes = listOf("Banjo-Kazooie (USA)", "Banjo-Kazooie (Europe) (En,Fr,De)", "Banjo-Tooie (Australia)", "Banjo-Kazooie (USA) (Rev 1)")
        assertEquals(
            listOf("Banjo-Kazooie (USA)", "Banjo-Kazooie (Europe) (En,Fr,De)", "Banjo-Kazooie (USA) (Rev 1)"),
            Libretro.casam("Banjo-Kazooie", nomes),
        )
    }

    @Test fun `jogo sem relacao nao casa`() {
        assertEquals(emptyList<String>(), Libretro.casam("Banjo-Kazooie", listOf("Sonic Mania (USA)")))
    }

    @Test fun `pokemon gold silver crystal casa o Gold pela primeira parte do titulo`() {
        val nomes = listOf("Pokemon - Gold Version (USA, Europe) (SGB Enhanced)", "Pokemon - Red Version (USA)")
        assertEquals(listOf("Pokemon - Gold Version (USA, Europe) (SGB Enhanced)"), Libretro.casam("Pokémon Gold / Silver / Crystal", nomes))
    }

    @Test fun `melhor casamento prefere o (USA) quando a nota empata`() {
        assertEquals("Banjo-Kazooie (USA)", Libretro.melhor("Banjo-Kazooie", listOf("Banjo-Kazooie (Europe)", "Banjo-Kazooie (USA)")))
        assertNull(Libretro.melhor("Banjo-Kazooie", listOf("Sonic Mania (USA)")))
    }

    @Test fun `console conhecido tem pasta e console desconhecido nao`() {
        assertEquals("Nintendo - Nintendo 64", Libretro.sistemaDe("Nintendo 64"))
        assertEquals("Sega - Mega Drive - Genesis", Libretro.sistemaDe("Mega Drive"))
        assertNull(Libretro.sistemaDe("Outros"))
    }

    @Test fun `endereco do png codifica o sistema e o nome`() {
        assertEquals(
            "https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%2064/Named_Snaps/Banjo-Kazooie%20%28USA%29.png",
            Libretro.arquivo("Nintendo - Nintendo 64", Libretro.SNAPS, "Banjo-Kazooie (USA)"),
        )
    }
}
