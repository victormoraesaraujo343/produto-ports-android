package app.ports.core

import java.text.Normalizer
import java.util.Locale

/**
 * Diz se um nome devolvido pelo SteamGridDB é o jogo que buscamos. A busca
 * do SteamGridDB devolve jogos parecidos; sem essa conta, o primeiro acerto
 * vira capa de outro jogo (era o caso do Pokémon Gold/Silver/Crystal).
 */
object Casamento {
    private const val LIMITE_JACCARD = 0.6
    private val pontuacao = Regex("[^\\p{L}\\p{N}\\s]")
    private val marcas = Regex("\\p{Mn}+")
    private val espacos = Regex("\\s+")

    /** Minúsculas, sem acento, sem pontuação e com espaços únicos. */
    fun normalizar(texto: String): String {
        val semAcento = marcas.replace(Normalizer.normalize(texto, Normalizer.Form.NFD), "")
        return espacos.replace(pontuacao.replace(semAcento.lowercase(Locale.ROOT), " "), " ").trim()
    }

    /**
     * Nota de 0 a 1 se `nome` é o jogo de `consulta`, ou nulo se não é. A
     * consulta vale inteira ou pela primeira parte (antes de " / "), porque
     * "Pokemon Gold / Silver / Crystal" é, na prática, o Pokémon Gold.
     *
     * - Nomes iguais depois de normalizar: 1.
     * - Um contém o outro, palavra por palavra ("Real Steel" e "Real Steel
     *   World Robot Boxing"): 0,9 mais a parte de Jaccard. Decisão: é o mesmo
     *   jogo, só com o subtítulo oficial.
     * - Senão, Jaccard sobre as palavras, aceito a partir de 0,6.
     */
    fun pontuar(consulta: String, nome: String): Double? {
        val alvo = normalizar(nome)
        if (alvo.isEmpty()) return null
        return listOf(consulta, consulta.substringBefore(" / "))
            .map(::normalizar)
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { notaUma(it, alvo) }
            .maxOrNull()
    }

    /** O item de maior nota que passa. Empate fica com o primeiro da lista. */
    fun <T> melhor(consulta: String, itens: List<T>, nome: (T) -> String): T? {
        var escolhido: T? = null
        var nota = -1.0
        for (item in itens) {
            val n = pontuar(consulta, nome(item)) ?: continue
            if (n > nota) {
                nota = n
                escolhido = item
            }
        }
        return escolhido
    }

    private fun notaUma(a: String, b: String): Double? {
        if (a == b) return 1.0
        val ta = a.split(" ").toSet()
        val tb = b.split(" ").toSet()
        val jaccard = ta.intersect(tb).size.toDouble() / ta.union(tb).size
        val contem = " $b ".contains(" $a ") || " $a ".contains(" $b ")
        return when {
            contem -> 0.9 + 0.1 * jaccard
            jaccard >= LIMITE_JACCARD -> jaccard
            else -> null
        }
    }
}
