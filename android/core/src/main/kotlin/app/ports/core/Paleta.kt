package app.ports.core

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Cores de fundo do placeholder de capa. Derivadas do id: o mesmo port sempre
 * tem a mesma arte, e ports diferentes têm cores diferentes. Sempre escuras.
 */
object Paleta {
    fun de(id: String): List<Long> {
        val h = hash(id)
        val matiz = (h % 360).toFloat()
        val saturacao = 0.40f + (h / 360 % 30) / 100f
        return listOf(
            hsl(matiz, saturacao, 0.26f),
            hsl((matiz + 24f) % 360f, saturacao, 0.13f),
            hsl((matiz + 336f) % 360f, saturacao * 0.6f, 0.06f),
        )
    }

    /** FNV-1a de 32 bits, sem sinal: estável entre execuções (ao contrário de hashCode em outras plataformas). */
    internal fun hash(texto: String): Int {
        var h = 0x811c9dc5.toInt()
        for (b in texto.toByteArray(Charsets.UTF_8)) {
            h = h xor (b.toInt() and 0xff)
            h *= 0x01000193
        }
        return h and 0x7fffffff
    }

    /** HSL (matiz em graus, saturação e luminosidade de 0 a 1) pra ARGB com alfa cheio. */
    private fun hsl(h: Float, s: Float, l: Float): Long {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun canal(v: Float): Long = ((v + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
        return 0xFF000000L or (canal(r) shl 16) or (canal(g) shl 8) or canal(b)
    }
}
