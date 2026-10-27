package app.ports.launcher.dados

import android.content.Context
import app.ports.core.Apks
import app.ports.core.Validade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Última release de um port no GitHub, com o APK do padrão do catálogo.
 * Cache de 6 h em memória e em `filesDir/releases.json`. Sem rede ou com o
 * limite do GitHub estourado, devolve o que já tinha (ou nulo).
 */
class GitHubReleases(contexto: Context) {
    private class Entrada(val buscadoEm: Long, val release: Release?)

    private val arquivo = File(contexto.filesDir, "releases.json")
    private val cache: MutableMap<String, Entrada> by lazy { carregarArquivo() }

    suspend fun ultima(repo: String, canal: String, padrao: String?): Release? = withContext(Dispatchers.IO) {
        val chave = "$repo|$canal|${padrao.orEmpty()}"
        val achado = synchronized(cache) { cache[chave] }
        val agora = System.currentTimeMillis()
        if (achado != null && Validade.fresco(achado.buscadoEm, agora, Validade.SEIS_HORAS)) return@withContext achado.release
        try {
            val novo = buscar(repo, canal, padrao)
            synchronized(cache) {
                cache[chave] = Entrada(agora, novo)
                salvar()
            }
            novo
        } catch (_: IOException) {
            // sem rede ou limite do GitHub (403): mantém o que tinha
            achado?.release
        } catch (_: JSONException) {
            achado?.release
        }
    }

    private fun buscar(repo: String, canal: String, padrao: String?): Release? {
        val prerelease = canal == "prerelease"
        val url = if (prerelease) "$API/repos/$repo/releases?per_page=10" else "$API/repos/$repo/releases/latest"
        val resposta = Rede.texto(url, mapOf("Accept" to "application/vnd.github+json"))
        when (resposta.codigo) {
            404 -> return null
            403, 429 -> throw IOException("limite do GitHub")
        }
        if (resposta.codigo !in 200..299) throw IOException("GitHub respondeu ${resposta.codigo}")
        val lancamento = if (prerelease) {
            JSONArray(resposta.corpo).paraObjetos().firstOrNull { !it.optBoolean("draft") } ?: return null
        } else {
            JSONObject(resposta.corpo)
        }
        val assets = lancamento.optJSONArray("assets")?.paraObjetos().orEmpty()
        val apk = Apks.primeiro(padrao, assets) { it.optString("name") } ?: return null
        return Release(
            tag = lancamento.optString("tag_name"),
            apkUrl = apk.getString("browser_download_url"),
            tamanhoBytes = apk.optLong("size"),
            publicadoEm = lancamento.optTexto("published_at"),
        )
    }

    private fun carregarArquivo(): MutableMap<String, Entrada> {
        val mapa = mutableMapOf<String, Entrada>()
        if (!arquivo.exists()) return mapa
        try {
            val raiz = JSONObject(arquivo.readText())
            for (chave in raiz.keys()) {
                val o = raiz.getJSONObject(chave)
                val r = o.optJSONObject("release")
                val release = r?.let {
                    Release(it.getString("tag"), it.getString("apkUrl"), it.getLong("tamanhoBytes"), it.optTexto("publicadoEm"))
                }
                mapa[chave] = Entrada(o.getLong("buscadoEm"), release)
            }
        } catch (_: Exception) {
            // arquivo corrompido: começa do zero
            mapa.clear()
        }
        return mapa
    }

    /** Chamado com o lock de `cache`. */
    private fun salvar() {
        val raiz = JSONObject()
        for ((chave, e) in cache) {
            val r = e.release
            val o = JSONObject().put("buscadoEm", e.buscadoEm)
            o.put(
                "release",
                if (r == null) JSONObject.NULL else JSONObject()
                    .put("tag", r.tag)
                    .put("apkUrl", r.apkUrl)
                    .put("tamanhoBytes", r.tamanhoBytes)
                    .put("publicadoEm", r.publicadoEm ?: JSONObject.NULL),
            )
            raiz.put(chave, o)
        }
        arquivo.writeText(raiz.toString())
    }

    private companion object {
        const val API = "https://api.github.com"
    }
}
