package app.ports.core

import java.util.regex.PatternSyntaxException

/** Escolha do APK entre os arquivos de uma release. */
object Apks {
    /**
     * O primeiro item cujo nome bate com o padrão (regex do catálogo). Sem
     * padrão, cai no primeiro `.apk`. Padrão inválido não acha nada.
     */
    fun <T> primeiro(padrao: String?, itens: List<T>, nome: (T) -> String): T? {
        if (padrao.isNullOrBlank()) return itens.firstOrNull { nome(it).endsWith(".apk", ignoreCase = true) }
        val regex = try {
            Regex(padrao)
        } catch (_: PatternSyntaxException) {
            return null
        }
        return itens.firstOrNull { regex.containsMatchIn(nome(it)) }
    }
}

object Versoes {
    /** Tira o "v" da tag do GitHub: `v1.3.2` vira `1.3.2`. */
    fun semV(tag: String): String = tag.trim().removePrefix("v")
}

object Tamanhos {
    /** Tamanho legível pra tela ("92 MB", "1.5 GB"). Zero ou negativo vira vazio. */
    fun formatar(bytes: Long): String {
        if (bytes <= 0L) return ""
        val mb = bytes / 1_048_576.0
        return when {
            mb < 1.0 -> "${maxOf(1, Math.round(bytes / 1024.0))} KB"
            mb < 1024.0 -> "${Math.round(mb)} MB"
            else -> String.format(java.util.Locale.ROOT, "%.1f GB", mb / 1024.0)
        }
    }
}
