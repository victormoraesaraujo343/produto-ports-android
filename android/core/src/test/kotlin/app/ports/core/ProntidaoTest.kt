package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ProntidaoTest {
    private val base = Fatos(instalado = true, versaoInstalada = "1.0", versaoMaisNova = "1.0", arquivo = Arquivo.CONFERIDO, rodaAqui = true)

    @Test fun `tudo certo fica pronto`() = assertEquals(Estado.PRONTO, Prontidao.calcular(base))

    @Test fun `nao rodar no aparelho vence tudo`() =
        assertEquals(Estado.NAO_RODA, Prontidao.calcular(base.copy(rodaAqui = false, instalado = false)))

    @Test fun `nao instalado fica disponivel`() =
        assertEquals(Estado.DISPONIVEL, Prontidao.calcular(base.copy(instalado = false, versaoInstalada = null, arquivo = Arquivo.FALTA)))

    @Test fun `baixando aparece mesmo com versao velha`() =
        assertEquals(Estado.BAIXANDO, Prontidao.calcular(base.copy(versaoMaisNova = "1.1", baixando = 40)))

    @Test fun `falta de arquivo vem antes de atualizacao`() =
        assertEquals(Estado.FALTA_ARQUIVO, Prontidao.calcular(base.copy(versaoMaisNova = "1.1", arquivo = Arquivo.FALTA)))

    @Test fun `arquivo que nao confere`() =
        assertEquals(Estado.ARQUIVO_ERRADO, Prontidao.calcular(base.copy(arquivo = Arquivo.NAO_CONFERE)))

    @Test fun `versao nova disponivel`() =
        assertEquals(Estado.ATUALIZAR, Prontidao.calcular(base.copy(versaoMaisNova = "1.1")))

    @Test fun `as palavras nao usam termos tecnicos`() {
        val proibidas = listOf("rom", "hash", "otr", "apk", "sha")
        for (e in Estado.entries) {
            val t = Textos.de(e)
            for (texto in listOf(t.curto, t.longo, t.acao)) {
                val palavras = texto.lowercase().split(Regex("[^a-zà-ú0-9]+"))
                for (p in proibidas) assert(p !in palavras) { "\"$texto\" usa \"$p\"" }
            }
        }
    }
}
