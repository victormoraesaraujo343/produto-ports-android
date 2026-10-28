package app.ports.core

/** Datas do catálogo, pra mostrar na tela. */
object Datas {
    private val iso = Regex("""(\d{4})-(\d{2})-(\d{2})(?:[T ].*)?""")

    /** Data ISO (yyyy-MM-dd…) como dd/MM; nulo se não der pra ler. */
    fun curta(valor: String?): String? {
        val m = iso.matchEntire(valor?.trim().orEmpty()) ?: return null
        val (_, mes, dia) = m.destructured
        val (mesN, diaN) = mes.toInt() to dia.toInt()
        if (mesN !in 1..12 || diaN !in 1..31) return null
        return "$dia/$mes"
    }
}
