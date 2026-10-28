package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JogosTest {
    private fun porta(
        id: String,
        titulo: String = id,
        estrelas: Int? = null,
        testado: Boolean = false,
        atualizadoEm: String? = null,
    ) = PortNaTela(
        Port(
            id = id, titulo = titulo, logo = "", nomePort = "", tipo = "", plataforma = "PC", repo = "",
            descricao = "", arquivoQueServe = "", tamanho = "", versaoMaisNova = null, pacote = null, cores = null,
            estrelas = estrelas, testado = testado, atualizadoEm = atualizadoEm,
        ),
        Estado.DISPONIVEL,
        null,
    )

    private fun instalada(p: PortNaTela) = p.copy(estado = Estado.PRONTO, versaoInstalada = "1.0")

    @Test fun `parenteses e pontuacao nao mudam a chave`() {
        assertEquals(Jogos.chave("Perfect Dark"), Jogos.chave("Perfect Dark (Perfect Dark Port [Android])"))
        assertEquals(Jogos.chave("Need for Speed Carbon"), Jogos.chave("Need for Speed: Carbon"))
    }

    @Test fun `acento e caixa nao mudam a chave`() {
        assertEquals(Jogos.chave("Pokemon Gold"), Jogos.chave("Pokémon GOLD"))
    }

    @Test fun `jogos diferentes continuam diferentes`() {
        assertTrue(Jogos.chave("Mario Kart 64 (SpaghettiKart)") != Jogos.chave("Mario Kart Wii"))
        assertEquals("mario kart 64", Jogos.chave("Mario Kart 64 (SpaghettiKart)"))
    }

    @Test fun `testado vence estrelas`() {
        val a = porta("a", estrelas = 100)
        val b = porta("b", estrelas = 1, testado = true)
        assertEquals("b", Jogos.sugerida(listOf(a, b)).port.id)
    }

    @Test fun `estrelas vencem a data`() {
        val a = porta("a", estrelas = 10, atualizadoEm = "2020-01-01T00:00:00Z")
        val b = porta("b", estrelas = 5, atualizadoEm = "2026-01-01T00:00:00Z")
        assertEquals("a", Jogos.sugerida(listOf(b, a)).port.id)
    }

    @Test fun `data vence a falta de data`() {
        val a = porta("a", atualizadoEm = "2024-01-01T00:00:00Z")
        val b = porta("b")
        assertEquals("a", Jogos.sugerida(listOf(b, a)).port.id)
    }

    @Test fun `fonte unica vira o jogo com ela como sugerida e principal`() {
        val unica = porta("a", titulo = "Sonic")
        val jogo = Jogos.agrupar(listOf(unica)).single()
        assertEquals(listOf(unica), jogo.fontes)
        assertEquals(unica, jogo.sugerida)
        assertEquals(unica, jogo.principal)
    }

    @Test fun `principal e a instalada mesmo quando nao e a sugerida`() {
        val sugerida = porta("a", titulo = "Doom", testado = true)
        val instalada = instalada(porta("b", titulo = "Doom (Port)"))
        val jogo = Jogos.agrupar(listOf(sugerida, instalada)).single()
        assertEquals("a", jogo.sugerida.port.id)
        assertEquals("b", jogo.principal.port.id)
        assertEquals("a", jogo.fontes.first().port.id)
    }

    @Test fun `com duas instaladas vence a de uso mais recente`() {
        val a = instalada(porta("a", titulo = "Doom", testado = true))
        val b = instalada(porta("b", titulo = "Doom"))
        val uso = mapOf("a" to 100L, "b" to 500L)
        val jogo = Jogos.agrupar(listOf(a, b)) { uso[it] ?: 0L }.single()
        assertEquals("b", jogo.principal.port.id)
    }

    @Test fun `sem instalada a principal e a que baixa, depois a sugerida`() {
        val sugerida = porta("a", titulo = "Doom", testado = true)
        val baixando = porta("b", titulo = "Doom").copy(estado = Estado.BAIXANDO, progresso = 40)
        assertEquals("b", Jogos.agrupar(listOf(sugerida, baixando)).single().principal.port.id)
        assertEquals("a", Jogos.agrupar(listOf(sugerida, porta("b", titulo = "Doom"))).single().principal.port.id)
    }

    @Test fun `a ordem dos jogos segue a primeira aparicao`() {
        val lista = listOf(
            porta("z1", titulo = "Ancient Game"),
            porta("m1", titulo = "Mario Kart"),
            porta("z2", titulo = "Ancient Game (Port)"),
            porta("m2", titulo = "Zelda"),
        )
        val jogos = Jogos.agrupar(lista)
        assertEquals(listOf("ancient game", "mario kart", "zelda"), jogos.map { it.chave })
        assertEquals(listOf("z1", "z2"), jogos[0].fontes.map { it.port.id }.sorted())
    }
}
