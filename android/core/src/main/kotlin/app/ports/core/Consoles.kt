package app.ports.core

import java.text.Collator
import java.util.Locale

/** Filtro por console na tela de Ports: o nome de cada console e a conta. */
object Consoles {
    /** Nome do console pra mostrar (ver `Plataformas.nome`): vazio, "other" ou "unknown" viram Outros. */
    fun nomeDe(plataforma: String): String = Plataformas.nome(plataforma)

    /** Consoles presentes, com a conta. Mais ports primeiro; empate por nome (pt-BR). Outros sempre por último. */
    fun agrupar(ports: List<PortNaTela>): List<Pair<String, Int>> {
        val ordem = Collator.getInstance(Locale("pt", "BR"))
        return ports.groupingBy { nomeDe(it.port.plataforma) }.eachCount().toList()
            .sortedWith(
                compareBy<Pair<String, Int>> { it.first == Plataformas.OUTROS }
                    .thenByDescending { it.second }
                    .thenComparator { a, b -> ordem.compare(a.first, b.first) },
            )
    }

    /** Só os ports do console escolhido. Nulo quer dizer todos. */
    fun filtrar(ports: List<PortNaTela>, console: String?): List<PortNaTela> =
        if (console == null) ports else ports.filter { nomeDe(it.port.plataforma) == console }
}
