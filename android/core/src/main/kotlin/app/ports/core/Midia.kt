package app.ports.core

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Imagens do README: acha `![alt](url)` e `<img src>`, resolve o caminho
 * relativo contra o endereço do README e deixa de fora SVG, badges e logos.
 */
object ImagensDoReadme {
    data class Achada(val alt: String, val url: String)

    // ![alt](url), ![alt](<url com espaço>) e ![alt](url "título")
    private val markdown = Regex("""!\[([^\]]*)\]\(\s*(?:<([^>]+)>|([^\s)]+))(?:\s+(?:"[^"]*"|'[^']*'))?\s*\)""")
    private val tagImg = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val src = Regex("""(?<![\w-])src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val alt = Regex("""(?<![\w-])alt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
    private val esquema = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:""")
    private val origem = Regex("""^https?://[^/]+""")
    private val blobGithub = Regex("""^https://github\.com/([^/]+)/([^/]+)/(?:blob|raw)/([^/?#]+)/([^?#]+)""")
    private val marcasDeBadge = listOf("shields.io", "badge")
    private val marcasDeLogo = listOf("logo", "icon", "banner", "sponsor", "donate", "discord", "kofi", "ko-fi", "patreon")

    /** Todas as imagens do texto, na ordem em que aparecem (markdown e HTML). Ainda sem resolver. */
    fun extrair(texto: String): List<Achada> {
        val achadas = mutableListOf<Pair<Int, Achada>>()
        for (m in markdown.findAll(texto)) {
            val url = m.groupValues[2].ifEmpty { m.groupValues[3] }
            achadas += m.range.first to Achada(m.groupValues[1].trim(), url)
        }
        for (m in tagImg.findAll(texto)) {
            val url = src.find(m.value)?.groupValues?.get(1) ?: continue
            achadas += m.range.first to Achada(alt.find(m.value)?.groupValues?.get(1).orEmpty(), url)
        }
        return achadas.sortedBy { it.first }.map { it.second }
    }

    /** URL absoluta da imagem, relativa ao `base` (o endereço do README). Nulo se não dá pra baixar. */
    fun resolver(base: String, url: String): String? {
        val bruto = url.trim()
        if (bruto.isEmpty() || bruto.startsWith("#") || bruto.startsWith("data:", ignoreCase = true)) return null
        val absoluta = when {
            bruto.startsWith("//") -> "https:$bruto"
            esquema.containsMatchIn(bruto) -> bruto
            else -> juntar(base, bruto) ?: return null
        }
        return paraRaw(absoluta)
    }

    /** Um relativo junto do diretório do `base`, com `.` e `..` resolvidos. Começando com `/`, parte da raiz do host. */
    private fun juntar(base: String, relativo: String): String? {
        val host = origem.find(base)?.value ?: return null
        val caminhoBase = base.removePrefix(host).substringBefore('?').substringBefore('#')
        val semQuery = relativo.substringBefore('?').substringBefore('#')
        val query = if ('?' in relativo) "?" + relativo.substringAfter('?').substringBefore('#') else ""
        val partes = ArrayList<String>()
        if (!semQuery.startsWith("/")) partes += caminhoBase.substringBeforeLast('/', "").split('/').filter { it.isNotEmpty() }
        for (seg in semQuery.split('/')) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (partes.isNotEmpty()) partes.removeAt(partes.lastIndex)
                else -> partes += seg
            }
        }
        return host + "/" + partes.joinToString("/") + query
    }

    /** Link de página do GitHub (`/blob/` ou `/raw/`) vira o arquivo cru, que é o que se baixa. */
    private fun paraRaw(url: String): String {
        val m = blobGithub.find(url) ?: return url
        val (dono, repo, ramo, caminho) = m.destructured
        return "https://raw.githubusercontent.com/$dono/$repo/$ramo/$caminho"
    }

    /** Fora SVG, badges (shields.io, workflows) e logos, ícones, banners e botões de apoio. Vale pelo caminho e pelo alt. */
    fun ehDeJogo(url: String, alt: String): Boolean {
        val caminho = url.substringBefore('?').substringBefore('#').lowercase()
        if (caminho.endsWith(".svg")) return false
        if (marcasDeBadge.any { caminho.contains(it) }) return false
        if (caminho.contains("github.com/") && caminho.contains("/workflows/")) return false
        val texto = caminho + " " + alt.lowercase()
        return marcasDeLogo.none { texto.contains(it) }
    }
}

/** Filtro das pastas de imagem do repositório (árvore do git) e o endereço cru de cada uma. */
object FiltroRepo {
    private val extensoes = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif")
    private val pastas = listOf("screenshot", "screens", "docs/", "assets/", "images/", "media/", "img/")
    private val recusadas = listOf("icon", "logo", "mipmap", "drawable", "/res/", "badge", "font", "ui/", "sprite", "-secondary")

    /** O caminho (a partir da raiz do repositório) é uma imagem de jogo? */
    fun ehImagemDeJogo(caminho: String): Boolean {
        val c = caminho.lowercase()
        return extensoes.any { c.endsWith(it) } &&
            pastas.any { c.contains(it) } &&
            recusadas.none { "/$c".contains(it) }
    }

    /** Endereço cru do arquivo no GitHub, com cada parte do caminho codificada. */
    fun urlRaw(repo: String, ramo: String, caminho: String): String =
        "https://raw.githubusercontent.com/$repo/$ramo/" + caminho.split('/').joinToString("/") { codificar(it) }
}

/** Capturas de jogo do libretro-thumbnails: console, listagem, casamento com o título e endereço. */
object Libretro {
    const val SNAPS = "Named_Snaps"
    const val TITULOS = "Named_Titles"
    private const val BASE = "https://thumbnails.libretro.com"

    /** Nome do console (`Plataformas.nome`) pra pasta do libretro. Console fora da lista não tem capturas. */
    private val sistemas = mapOf(
        "Nintendo 64" to "Nintendo - Nintendo 64",
        "Super Nintendo" to "Nintendo - Super Nintendo Entertainment System",
        "Game Boy" to "Nintendo - Game Boy",
        "Game Boy Color" to "Nintendo - Game Boy Color",
        "Game Boy Advance" to "Nintendo - Game Boy Advance",
        "Nintendo DS" to "Nintendo - Nintendo DS",
        "GameCube" to "Nintendo - GameCube",
        "Wii" to "Nintendo - Wii",
        "Mega Drive" to "Sega - Mega Drive - Genesis",
        "PlayStation" to "Sony - PlayStation",
        "PlayStation 2" to "Sony - PlayStation 2",
        "Xbox" to "Microsoft - Xbox",
        "Xbox 360" to "Microsoft - Xbox 360",
    )
    private val href = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    fun sistemaDe(plataforma: String): String? = sistemas[plataforma]

    /** Endereço da listagem de uma pasta (`Named_Snaps` ou `Named_Titles`) de um sistema. */
    fun pasta(sistema: String, tipo: String): String = "$BASE/${codificar(sistema)}/$tipo/"

    /** Endereço do PNG de um nome listado (sem `.png`). */
    fun arquivo(sistema: String, tipo: String, nome: String): String = "${pasta(sistema, tipo)}${codificar(nome)}.png"

    /** Nomes (sem `.png`, já decodificados) dos PNGs de uma listagem HTML de diretório. */
    fun nomesDaListagem(html: String): List<String> =
        href.findAll(html).map { it.groupValues[1] }
            .filter { it.endsWith(".png", ignoreCase = true) && !it.contains('/') }
            .map { decodificar(it).dropLast(4).replace("&amp;", "&") }
            .distinct()
            .toList()

    /** Os nomes que são o jogo do `titulo` (mesma regra do Casamento), de todas as regiões e versões. */
    fun casam(titulo: String, nomes: List<String>): List<String> = nomes.filter { pontuar(titulo, it) != null }

    /**
     * O nome de maior nota pro `titulo`, ou nulo. Empate vai pro nome com (USA),
     * e depois pelo primeiro da lista.
     */
    fun melhor(titulo: String, nomes: List<String>): String? =
        nomes.mapNotNull { nome -> pontuar(titulo, nome)?.let { nome to it } }
            .sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { !it.first.contains("(USA") })
            .firstOrNull()?.first

    /** Tira as tags de região e revisão antes de comparar: "Banjo-Kazooie (USA) (Rev 1)" vira "Banjo-Kazooie". */
    private fun pontuar(titulo: String, nome: String): Double? {
        val alvo = Titulo.semParenteses(nome)
        return Titulo.consultas(titulo).mapNotNull { Casamento.pontuar(it, alvo) }.maxOrNull()
    }
}

private fun codificar(texto: String): String = URLEncoder.encode(texto, "UTF-8").replace("+", "%20")

private fun decodificar(texto: String): String =
    try {
        URLDecoder.decode(texto.replace("+", "%2B"), "UTF-8")
    } catch (_: IllegalArgumentException) {
        texto
    }
