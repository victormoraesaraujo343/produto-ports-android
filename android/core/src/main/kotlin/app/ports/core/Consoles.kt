package app.ports.core

import java.text.Collator
import java.util.Locale

/** Filtro por console na tela de Ports: o nome de cada console e a conta. */
object Consoles {
    const val OUTROS = "Outros"

    /** Nome do console pra mostrar. Vazio, "other" ou "unknown" (como nos Detalhes) vira "Outros". */
    fun nomeDe(plataforma: String): String {
        val nome = plataforma.trim()
        return if (nome.isEmpty() || nome.equals("other", true) || nome.equals("unknown", true)) OUTROS else nome
    }

    /** Consoles presentes, com a conta. Mais ports primeiro; empate por nome (pt-BR). */
    fun agrupar(ports: List<PortNaTela>): List<Pair<String, Int>> {
        val ordem = Collator.getInstance(Locale("pt", "BR"))
        return ports.groupingBy { nomeDe(it.port.plataforma) }.eachCount().toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenComparator { a, b -> ordem.compare(a.first, b.first) })
    }

    /** Só os ports do console escolhido. Nulo quer dizer todos. */
    fun filtrar(ports: List<PortNaTela>, console: String?): List<PortNaTela> =
        if (console == null) ports else ports.filter { nomeDe(it.port.plataforma) == console }
}
