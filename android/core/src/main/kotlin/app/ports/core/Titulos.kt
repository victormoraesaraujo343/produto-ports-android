package app.ports.core

import java.text.Normalizer
import java.util.Locale

/** Títulos do catálogo: a logo da capa e a busca no SteamGridDB. */
object Titulo {
    private val entreParenteses = Regex("\\([^()]*\\)|\\[[^\\[\\]]*\\]")
    private val ruido = Regex(
        "\\b(recomp|recompiled|recompilation|decomp|decompilation|port|pc|native|remake|project)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val espacos = Regex("\\s+")

    /** Tira os trechos entre parênteses e colchetes, inclusive os aninhados. */
    fun semParenteses(titulo: String): String {
        var atual = titulo
        while (true) {
            val novo = entreParenteses.replace(atual, " ")
            if (novo == atual) break
            atual = novo
        }
        return espacos.replace(atual, " ").trim()
    }

    /** A logo da capa: sem parênteses, em maiúsculas. */
    fun logo(titulo: String): String = semParenteses(titulo).uppercase(Locale.ROOT)

    /** O título pra buscar no SteamGridDB: sem parênteses nem as palavras de recompilação. */
    fun limparParaBusca(titulo: String): String {
        val semRuido = espacos.replace(ruido.replace(semParenteses(titulo), " "), " ")
        return semRuido.trim { it.isWhitespace() || it in "-–:,|" }
    }

    /**
     * Buscas no SteamGridDB, da mais completa pra mais curta. Com " / " (vários
     * jogos no título), tenta também a primeira parte sozinha. "Pokémon" vai
     * sem acento, como o SteamGridDB costuma indexar.
     */
    fun consultas(titulo: String): List<String> {
        val limpo = limparParaBusca(titulo)
        val partes = if (limpo.contains(" / ")) listOf(limpo, limpo.substringBefore(" / ").trim()) else listOf(limpo)
        return partes.map(::arrumar).filter { it.isNotBlank() }.distinct()
    }

    /**
     * Buscas por jogo, uma por parte de um título com " / " ("Pokemon Gold / Silver /
     * Crystal" vira Pokemon Gold, Pokemon Silver, Pokemon Crystal). Parte de uma
     * palavra só ganha a franquia da primeira ("Silver" vira "Pokemon Silver").
     * Sem " / ", é a própria busca.
     */
    fun porParte(titulo: String): List<String> {
        val partes = limparParaBusca(titulo).split(" / ").map { it.trim() }.filter { it.isNotEmpty() }
        if (partes.size < 2) return consultas(titulo)
        val franquia = partes[0].split(" ").dropLast(1).joinToString(" ")
        val buscas = listOf(partes[0]) + partes.drop(1).map { if (franquia.isNotEmpty() && !it.contains(" ")) "$franquia $it" else it }
        return buscas.map(::arrumar).filter { it.isNotBlank() }.distinct()
    }

    /** NFC e "Pokémon" sem acento, como o SteamGridDB e o libretro costumam indexar. */
    private fun arrumar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFC).replace("Pokémon", "Pokemon", ignoreCase = true)
}

/** Nomes de plataforma pra mostrar na tela. Id desconhecido volta como veio. */
object Plataformas {
    const val OUTROS = "Outros"

    private val nomes = mapOf(
        "n64" to "Nintendo 64",
        "snes" to "Super Nintendo",
        "nes" to "NES",
        "gc" to "GameCube",
        "gamecube" to "GameCube",
        "ps1" to "PlayStation",
        "psx" to "PlayStation",
        "ps2" to "PlayStation 2",
        "ps3" to "PlayStation 3",
        "ps4" to "PlayStation 4",
        "psp" to "PSP",
        "xbox" to "Xbox",
        "xbox360" to "Xbox 360",
        "wii" to "Wii",
        "wiiu" to "Wii U",
        "switch" to "Nintendo Switch",
        "gb" to "Game Boy",
        "gbc" to "Game Boy Color",
        "gba" to "Game Boy Advance",
        "nds" to "Nintendo DS",
        "ds" to "Nintendo DS",
        "dreamcast" to "Dreamcast",
        "genesis" to "Mega Drive",
        "arcade" to "Arcade",
        "pc" to "PC",
    )

    /** `other`, vazio e `unknown` (e o que não tem plataforma) viram Outros. */
    fun nome(id: String): String {
        val limpo = id.trim()
        if (limpo.isEmpty() || limpo.equals("other", true) || limpo.equals("unknown", true)) return OUTROS
        return nomes[limpo.lowercase(Locale.ROOT)] ?: limpo
    }
}
