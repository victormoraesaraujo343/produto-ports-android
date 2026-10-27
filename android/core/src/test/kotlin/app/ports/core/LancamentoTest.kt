package app.ports.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LancamentoTest {
    private val arquivos = listOf("HuntersRecomp-v1.2.0-x86.apk", "HuntersRecomp-v1.2.0-thor-arm64.apk", "fonte.zip")
    private val padrao = "^HuntersRecomp-v\\d+\\.\\d+\\.\\d+-thor-arm\\d+\\.apk$"

    @Test fun `escolhe o apk que bate com o padrao`() =
        assertEquals("HuntersRecomp-v1.2.0-thor-arm64.apk", Apks.primeiro(padrao, arquivos) { it })

    @Test fun `sem apk que bata devolve nulo`() = assertNull(Apks.primeiro("^nada\\.apk$", arquivos) { it })

    @Test fun `padrao invalido nao acha nada`() = assertNull(Apks.primeiro("([", arquivos) { it })

    @Test fun `sem padrao cai no primeiro apk`() =
        assertEquals("HuntersRecomp-v1.2.0-x86.apk", Apks.primeiro(null, arquivos) { it })

    @Test fun `tira o v da tag`() {
        assertEquals("1.3.2", Versoes.semV("v1.3.2"))
        assertEquals("1.3.2", Versoes.semV("1.3.2"))
    }

    @Test fun `tamanho legivel`() {
        assertEquals("92 MB", Tamanhos.formatar(92L * 1_048_576))
        assertEquals("1.5 GB", Tamanhos.formatar(1536L * 1_048_576))
        assertEquals("", Tamanhos.formatar(0L))
    }
}
