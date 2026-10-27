package app.ports.core

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
}

/** Nomes de plataforma pra mostrar na tela. Id desconhecido volta como veio. */
object Plataformas {
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

    fun nome(id: String): String = nomes[id.lowercase(Locale.ROOT)] ?: id
}
