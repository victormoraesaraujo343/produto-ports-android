package app.ports.core

import java.text.Normalizer
import java.util.Locale

/** Um jogo na lista: os ports dele (fontes) e o que a capa mostra. */
data class JogoNaTela(val chave: String, val fontes: List<PortNaTela>, val principal: PortNaTela, val sugerida: PortNaTela)

object Jogos {
    private val diacriticos = Regex("\\p{Mn}+")
    private val naoLetraNumero = Regex("[^\\p{L}\\p{N}]+")

    /** Chave do jogo: título sem parênteses, sem acento, minúsculo, só letras e dígitos separados por espaço. */
    fun chave(titulo: String): String {
        val semAcento = diacriticos.replace(Normalizer.normalize(Titulo.semParenteses(titulo), Normalizer.Form.NFD), "")
        return naoLetraNumero.replace(semAcento.lowercase(Locale.ROOT), " ").trim()
    }

    /** Ordem da fonte sugerida: testado, depois estrelas, depois data mais nova, depois id. */
    private val ordemSugestao: Comparator<PortNaTela> = compareByDescending<PortNaTela> { it.port.testado }
        .thenByDescending { it.port.estrelas ?: -1 }
        .thenComparator { a, b -> dataMaisNovaPrimeiro(a.port.atualizadoEm, b.port.atualizadoEm) }
        .thenBy { it.port.id }

    /** Data ISO mais nova primeiro; sem data vai pro fim. */
    private fun dataMaisNovaPrimeiro(a: String?, b: String?): Int = when {
        a == b -> 0
        a == null -> 1
        b == null -> -1
        else -> b.compareTo(a)
    }

    /** A fonte que a tela sugere: a primeira pela ordem de sugestão. */
    fun sugerida(fontes: List<PortNaTela>): PortNaTela = fontes.sortedWith(ordemSugestao).first()

    /**
     * Agrupa os ports por jogo, mantendo a ordem de primeira aparição (a lista já vem A–Z).
     * `principal` é uma fonte instalada (a de uso mais recente); sem instalada, a que baixa; sem nenhuma, a sugerida.
     */
    fun agrupar(lista: List<PortNaTela>, usoRecente: (String) -> Long = { 0L }): List<JogoNaTela> {
        val porChave = LinkedHashMap<String, MutableList<PortNaTela>>()
        for (p in lista) porChave.getOrPut(chave(p.port.titulo)) { mutableListOf() }.add(p)
        return porChave.map { (chave, grupo) ->
            val fontes = grupo.sortedWith(ordemSugestao)
            val sugerida = sugerida(grupo)
            val candidatas = fontes.filter { it.versaoInstalada != null }
                .ifEmpty { fontes.filter { it.estado == Estado.BAIXANDO } }
                .ifEmpty { listOf(sugerida) }
            val principal = candidatas.sortedByDescending { usoRecente(it.port.id) }.first()
            JogoNaTela(chave, fontes, principal, sugerida)
        }
    }
}
