package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagensDoReadmeTest {
    private val base = "https://raw.githubusercontent.com/dono/repo/main/README.md"

    @Test fun `extrai markdown e html na ordem em que aparecem`() {
        val texto = """
            Intro ![Gameplay](docs/a.png)
            <img alt="Tela" src="img/b.jpg" width="300">
            ![Com título](<c d.png> "titulo")
        """.trimIndent()
        val achadas = ImagensDoReadme.extrair(texto)
        assertEquals(listOf("docs/a.png", "img/b.jpg", "c d.png"), achadas.map { it.url })
        assertEquals(listOf("Gameplay", "Tela", "Com título"), achadas.map { it.alt })
    }

    @Test fun `src de data-src nao conta`() {
        val achadas = ImagensDoReadme.extrair("""<img data-src="errado.png" src="certo.png">""")
        assertEquals(listOf("certo.png"), achadas.map { it.url })
    }

    @Test fun `caminho relativo sai do diretorio do README`() {
        assertEquals("https://raw.githubusercontent.com/dono/repo/main/docs/a.png", ImagensDoReadme.resolver(base, "docs/a.png"))
        assertEquals("https://raw.githubusercontent.com/dono/repo/main/docs/a.png", ImagensDoReadme.resolver(base, "./docs/a.png"))
    }

    @Test fun `subindice e ponto ponto voltam certo`() {
        val sub = "https://raw.githubusercontent.com/dono/repo/main/sub/README.md"
        assertEquals("https://raw.githubusercontent.com/dono/repo/main/img/c.png", ImagensDoReadme.resolver(sub, "../img/c.png"))
        assertEquals("https://raw.githubusercontent.com/dono/repo/main/sub/x/d.png", ImagensDoReadme.resolver(sub, "x/d.png"))
    }

    @Test fun `barra no comeco parte da raiz e absoluta fica como esta`() {
        assertEquals("https://raw.githubusercontent.com/abs/d.png", ImagensDoReadme.resolver(base, "/abs/d.png"))
        assertEquals("https://exemplo.com/x.png", ImagensDoReadme.resolver(base, "https://exemplo.com/x.png"))
        assertEquals("https://cdn.com/x.png", ImagensDoReadme.resolver(base, "//cdn.com/x.png"))
    }

    @Test fun `ancora e data uri nao viram imagem`() {
        assertNull(ImagensDoReadme.resolver(base, "#ancora"))
        assertNull(ImagensDoReadme.resolver(base, "data:image/png;base64,AAAA"))
        assertNull(ImagensDoReadme.resolver(base, "  "))
    }

    @Test fun `link de blob do GitHub vira o arquivo cru`() {
        assertEquals(
            "https://raw.githubusercontent.com/dono/repo/main/shots/a.png",
            ImagensDoReadme.resolver(base, "https://github.com/dono/repo/blob/main/shots/a.png?raw=true"),
        )
    }

    @Test fun `svg, badge e workflow ficam de fora`() {
        assertFalse(ImagensDoReadme.ehDeJogo("https://exemplo.com/tela.svg", "Tela"))
        assertFalse(ImagensDoReadme.ehDeJogo("https://img.shields.io/badge/build-ok-green.png", "Build"))
        assertFalse(ImagensDoReadme.ehDeJogo("https://github.com/dono/repo/workflows/ci/badge.png", "CI"))
    }

    @Test fun `logo, icone e apoio pelo caminho ou pelo alt ficam de fora`() {
        assertFalse(ImagensDoReadme.ehDeJogo("https://raw.githubusercontent.com/d/r/main/docs/logo.png", "Port"))
        assertFalse(ImagensDoReadme.ehDeJogo("https://raw.githubusercontent.com/d/r/main/docs/x.png", "Discord"))
        assertFalse(ImagensDoReadme.ehDeJogo("https://raw.githubusercontent.com/d/r/main/docs/x.png", "Apoie no Ko-fi"))
        assertTrue(ImagensDoReadme.ehDeJogo("https://raw.githubusercontent.com/d/r/main/docs/gameplay.png?v=2", "Gameplay"))
    }
}
