package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroRepoTest {
    @Test fun `aceita imagens de pasta de jogo`() {
        assertTrue(FiltroRepo.ehImagemDeJogo("docs/tela1.png"))
        assertTrue(FiltroRepo.ehImagemDeJogo("screenshots/a.JPG"))
        assertTrue(FiltroRepo.ehImagemDeJogo("screenshots/x.webp"))
        assertTrue(FiltroRepo.ehImagemDeJogo("docs/images/y.png"))
        assertTrue(FiltroRepo.ehImagemDeJogo("media/loop.gif"))
    }

    @Test fun `imagem fora de pasta de jogo fica de fora`() {
        assertFalse(FiltroRepo.ehImagemDeJogo("src/main/java/a.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/a.svg"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/notas.txt"))
    }

    @Test fun `pasta assets sozinha nao conta (arte que nao e jogo)`() {
        assertFalse(FiltroRepo.ehImagemDeJogo("assets/pokewalker/course_beach.jpg"))
        assertFalse(FiltroRepo.ehImagemDeJogo("assets/android-dual-screen/main-menu-primary.png"))
    }

    @Test fun `tela secondary fica de fora`() {
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/assets/android-dual-screen/slot-select-secondary.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("screenshots/stats-secondary.png"))
    }

    @Test fun `icone, logo, recurso de android e fonte ficam de fora`() {
        assertFalse(FiltroRepo.ehImagemDeJogo("assets/icon.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/logo.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("app/src/main/res/mipmap-hdpi/ic.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/drawable/x.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("res/drawable/x.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/fonts/a.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/ui/menu.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/sprite.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("docs/badge.png"))
    }

    @Test fun `endereco cru codifica cada parte do caminho`() {
        assertEquals(
            "https://raw.githubusercontent.com/dono/repo/main/docs/Tela%201%20%28a%29.png",
            FiltroRepo.urlRaw("dono/repo", "main", "docs/Tela 1 (a).png"),
        )
    }

    @Test fun `imagem da documentacao de biblioteca embutida fica de fora`() {
        assertFalse(FiltroRepo.ehImagemDeJogo("mobile/android/love/src/jni/LuaJIT-2.1/doc/img/contact.png"))
        assertFalse(FiltroRepo.ehImagemDeJogo("vendor/sdl/docs/images/logo.png"))
        assertTrue(FiltroRepo.ehImagemDeJogo("docs/images/gameplay-1.png"))
    }
}
