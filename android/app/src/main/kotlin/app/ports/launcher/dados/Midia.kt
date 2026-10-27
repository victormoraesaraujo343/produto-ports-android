package app.ports.launcher.dados

import android.content.Context
import app.ports.core.FiltroRepo
import app.ports.core.ImagensDoReadme
import app.ports.core.Libretro
import app.ports.core.Port
import app.ports.core.Validade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64

private const val MAXIMO = 10

/** Até 5 imagens do próprio repositório; o resto da galeria é de capturas do jogo e da tela de título. */
private const val REPOSITORIO_MAXIMO = 5

/**
 * Galeria de gameplay do Detalhes, na ordem: imagens do próprio port (README e
 * pastas de imagem do repositório), capturas do jogo no libretro-thumbnails
 * (sem repetidas) e, por último, a tela de título. Até 10 imagens.
 *
 * Cache: a galeria vale 24 h (também a que não achou nada, mas não a que falhou
 * por rede); as listagens do libretro, 7 dias; o sha1 das capturas fica salvo.
 * Em memória, o que foi lido dos arquivos; em disco, em `filesDir`.
 */
class Midia(contexto: Context) {
    // v2: o cache antigo tinha as imagens do assets/ (arte do Pokéwalker) e nenhuma captura
    private val galeriaArq = File(contexto.filesDir, "galeria-v3.json")
    init {
        File(contexto.filesDir, "galeria.json").delete(); File(contexto.filesDir, "galeria-v2.json").delete()
    }
    private val libretroArq = File(contexto.filesDir, "midia-libretro.json")
    private val hashesArq = File(contexto.filesDir, "midia-hashes.json")
    private val pasta = File(contexto.cacheDir, "midia").apply { mkdirs() }
    private val lock = Any()
    private val galeria by lazy { lerJson(galeriaArq) }
    private val libretro by lazy { lerJson(libretroArq) }
    private val hashes by lazy { lerJson(hashesArq) }

    /** URLs da galeria do port. Vazia se não acha nada. */
    suspend fun galeria(p: Port): List<String> = withContext(Dispatchers.IO) {
        doCache(p.id)?.let { return@withContext it }
        val (urls, completo) = montar(p)
        if (completo) gravarCache(p.id, urls)
        urls
    }

    private fun doCache(id: String): List<String>? {
        synchronized(lock) {
            val o = galeria.optJSONObject(id) ?: return null
            if (!Validade.fresco(o.optLong("buscadoEm"), System.currentTimeMillis(), Validade.VINTE_QUATRO_HORAS)) return null
            return o.optJSONArray("urls")?.textos().orEmpty()
        }
    }

    private fun gravarCache(id: String, urls: List<String>) {
        synchronized(lock) {
            galeria.put(id, JSONObject().put("buscadoEm", System.currentTimeMillis()).put("urls", JSONArray(urls)))
            salvar(galeriaArq, galeria)
        }
    }

    /** Junta as fontes na ordem da galeria. `completo` vira falso se uma fonte caiu por rede. */
    private fun montar(p: Port): Pair<List<String>, Boolean> {
        var completo = true
        val doRepositorio = LinkedHashSet<String>()
        try { doRepositorio += doReadme(p.repo) } catch (_: IOException) { completo = false }
        try { doRepositorio += doArvore(p.repo) } catch (_: IOException) { completo = false }

        val sistema = Libretro.sistemaDe(p.plataforma) ?: return doRepositorio.take(REPOSITORIO_MAXIMO) to completo
        val titulo = try {
            tituloDaCaptura(sistema, p.titulo)
        } catch (_: IOException) {
            completo = false
            null
        }
        // a tela de título sempre entra por último, então guarda uma vaga pra ela
        val reserva = if (titulo != null) 1 else 0
        val repoParte = doRepositorio.take(REPOSITORIO_MAXIMO)
        val vagas = MAXIMO - reserva - repoParte.size
        val capturas = if (vagas <= 0) emptyList() else try {
            capturasUnicas(sistema, p.titulo, vagas) { completo = false }
        } catch (_: IOException) {
            completo = false
            emptyList()
        }
        return (repoParte + capturas + listOfNotNull(titulo)) to completo
    }

    /** Imagens do README (markdown e HTML), já com o endereço resolvido e sem badge nem logo. 403 ou 404 só pula. */
    private fun doReadme(repo: String): List<String> {
        val r = Rede.texto("$API/repos/$repo/readme", CABECALHO)
        if (r.codigo !in 200..299) return emptyList()
        return try {
            val raiz = JSONObject(r.corpo)
            val texto = String(Base64.getMimeDecoder().decode(raiz.optString("content")), Charsets.UTF_8)
            val base = raiz.optTexto("download_url") ?: "https://raw.githubusercontent.com/$repo/HEAD/README.md"
            ImagensDoReadme.extrair(texto).mapNotNull { achada ->
                ImagensDoReadme.resolver(base, achada.url)?.takeIf { ImagensDoReadme.ehDeJogo(it, achada.alt) }
            }
        } catch (_: JSONException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    /** Imagens de jogo nas pastas de imagem do repositório. Lista truncada pelo GitHub: usa o que veio. */
    private fun doArvore(repo: String): List<String> {
        val info = Rede.texto("$API/repos/$repo", CABECALHO)
        if (info.codigo !in 200..299) return emptyList()
        val ramo = (try { JSONObject(info.corpo).optTexto("default_branch") } catch (_: JSONException) { null }) ?: return emptyList()
        val arvore = Rede.texto("$API/repos/$repo/git/trees/$ramo?recursive=1", CABECALHO)
        if (arvore.codigo !in 200..299) return emptyList()
        return try {
            JSONObject(arvore.corpo).optJSONArray("tree")?.paraObjetos().orEmpty()
                .filter { it.optString("type") == "blob" }
                .map { it.optString("path") }
                .filter { FiltroRepo.ehImagemDeJogo(it) }
                .map { FiltroRepo.urlRaw(repo, ramo, it) }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun tituloDaCaptura(sistema: String, titulo: String): String? {
        val nome = Libretro.melhor(titulo, listagem(sistema, Libretro.TITULOS)) ?: return null
        return Libretro.arquivo(sistema, Libretro.TITULOS, nome)
    }

    /**
     * Capturas do jogo sem repetir imagem. Baixa o que falta até juntar `vagas`.
     * Uma captura que falha só sai da lista (e avisa por `falhou`).
     */
    private fun capturasUnicas(sistema: String, titulo: String, vagas: Int, falhou: () -> Unit): List<String> {
        val candidatas = Libretro.casamIntercalado(titulo, listagem(sistema, Libretro.SNAPS))
            .map { Libretro.arquivo(sistema, Libretro.SNAPS, it) }
        val vistas = HashSet<String>()
        val saida = ArrayList<String>()
        for (url in candidatas) {
            if (saida.size >= vagas) break
            val hash = try {
                hashDe(url)
            } catch (_: IOException) {
                falhou()
                null
            } ?: continue
            if (vistas.add(hash)) saida += url
        }
        return saida
    }

    /** sha1 do PNG: vem do cache, ou baixa, calcula e guarda. */
    private fun hashDe(url: String): String {
        synchronized(lock) {
            hashes.optTexto(url)?.let { return it }
        }
        val temp = File(pasta, sha1(url.toByteArray()) + ".tmp")
        try {
            Rede.baixar(url, temp)
            val hash = sha1(temp.readBytes())
            synchronized(lock) {
                hashes.put(url, hash)
                salvar(hashesArq, hashes)
            }
            return hash
        } finally {
            temp.delete()
        }
    }

    /** Nomes de uma pasta do libretro, com cache de 7 dias. Pasta que não existe vira lista vazia. */
    private fun listagem(sistema: String, tipo: String): List<String> {
        val chave = "$sistema|$tipo"
        synchronized(lock) {
            val o = libretro.optJSONObject(chave)
            if (o != null && Validade.fresco(o.optLong("buscadoEm"), System.currentTimeMillis(), Validade.SETE_DIAS)) {
                return o.optJSONArray("nomes")?.textos().orEmpty()
            }
        }
        val r = Rede.texto(Libretro.pasta(sistema, tipo))
        val nomes = when (r.codigo) {
            in 200..299 -> Libretro.nomesDaListagem(r.corpo)
            404 -> emptyList()
            else -> throw IOException("libretro respondeu ${r.codigo}")
        }
        synchronized(lock) {
            libretro.put(chave, JSONObject().put("buscadoEm", System.currentTimeMillis()).put("nomes", JSONArray(nomes)))
            salvar(libretroArq, libretro)
        }
        return nomes
    }

    private companion object {
        const val API = "https://api.github.com"
        val CABECALHO = mapOf("Accept" to "application/vnd.github+json")
    }
}

private fun lerJson(arquivo: File): JSONObject = try {
    JSONObject(arquivo.readText())
} catch (_: Exception) {
    JSONObject()
}

private fun salvar(arquivo: File, json: JSONObject) {
    arquivo.writeText(json.toString())
}

private fun JSONArray.textos(): List<String> = (0 until length()).map { optString(it) }

private fun sha1(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
