package app.ports.core

/** Regras de validade dos caches locais (catálogo, releases, capas). */
object Validade {
    const val SEIS_HORAS = 6L * 60 * 60 * 1000
    const val SETE_DIAS = 7L * 24 * 60 * 60 * 1000

    /**
     * Verdadeiro se `buscadoEm` (ms) tem menos de `idadeMaxima` em relação a
     * `agora`. Sem marca, ou com o relógio voltando no tempo, não é fresco.
     */
    fun fresco(buscadoEm: Long, agora: Long, idadeMaxima: Long): Boolean {
        if (buscadoEm <= 0L) return false
        val idade = agora - buscadoEm
        return idade >= 0 && idade < idadeMaxima
    }
}
