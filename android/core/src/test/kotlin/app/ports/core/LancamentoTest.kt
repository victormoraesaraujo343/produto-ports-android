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

    private val padraoApk = "\\.apk$"
    private fun candidata(tag: String, preliminar: Boolean = false, rascunho: Boolean = false, nomes: List<String> = emptyList()) =
        Lancamentos.Candidata(tag, rascunho, preliminar, nomes)

    @Test fun `release mais nova so com windows cede pra anterior com apk`() {
        val candidatas = listOf(
            candidata("v2.0", nomes = listOf("Forza-windows.zip")),
            candidata("v1.9", nomes = listOf("Forza-android.apk")),
        )
        assertEquals("v1.9", Lancamentos.escolher("latest", padraoApk, candidatas))
    }

    @Test fun `release mais nova vazia cede pra anterior com apk`() {
        val candidatas = listOf(candidata("v2.0"), candidata("v1.9", nomes = listOf("Forza-android.apk")))
        assertEquals("v1.9", Lancamentos.escolher("latest", padraoApk, candidatas))
    }

    @Test fun `canal latest prefere estavel a prerelease mais nova`() {
        val candidatas = listOf(
            candidata("v3.0-rc1", preliminar = true, nomes = listOf("Forza-android.apk")),
            candidata("v2.0", nomes = listOf("Forza-android.apk")),
        )
        assertEquals("v2.0", Lancamentos.escolher("latest", padraoApk, candidatas))
    }

    @Test fun `canal latest usa prerelease so quando nenhuma estavel bate`() {
        val candidatas = listOf(
            candidata("v3.0-rc1", preliminar = true, nomes = listOf("Forza-android.apk")),
            candidata("v2.0", nomes = listOf("Forza-windows.zip")),
        )
        assertEquals("v3.0-rc1", Lancamentos.escolher("latest", padraoApk, candidatas))
    }

    @Test fun `canal prerelease pega a mais nova que bate, estavel ou nao`() {
        val candidatas = listOf(
            candidata("v3.0-rc1", preliminar = true, nomes = listOf("Forza-android.apk")),
            candidata("v2.0", nomes = listOf("Forza-android.apk")),
        )
        assertEquals("v3.0-rc1", Lancamentos.escolher("prerelease", padraoApk, candidatas))
    }

    @Test fun `rascunho nunca entra`() {
        val candidatas = listOf(
            candidata("v3.0", rascunho = true, nomes = listOf("Forza-android.apk")),
            candidata("v2.0", nomes = listOf("Forza-android.apk")),
        )
        assertEquals("v2.0", Lancamentos.escolher("latest", padraoApk, candidatas))
    }

    @Test fun `sem nenhum apk que bata devolve nulo`() {
        val candidatas = listOf(candidata("v2.0", nomes = listOf("Forza-windows.zip")), candidata("v1.0"))
        assertNull(Lancamentos.escolher("latest", padraoApk, candidatas))
    }
}
