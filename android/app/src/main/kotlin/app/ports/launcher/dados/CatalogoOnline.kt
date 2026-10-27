package app.ports.launcher.dados

import android.content.Context
import app.ports.core.Paleta
import app.ports.core.Plataformas
import app.ports.core.Port
import app.ports.core.Titulo
import app.ports.core.Validade
import app.ports.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * O ports-catalogue online (o mesmo do Hydra), só com as entradas que têm
 * `assets.android`. Cache em `filesDir/catalogo.json`, válido por 6 h.
 */
class CatalogoOnline(private val contexto: Context) {
    class Resultado(val ports: List<Port>, val atualizadoEm: String?, val doCache: Boolean)

    private val arquivo = File(contexto.filesDir, "catalogo.json")
    private val prefs = contexto.getSharedPreferences("catalogo", Context.MODE_PRIVATE)

    /**
     * Usa o cache se ele ainda é válido (ou se `forcar` é falso e ele existe).
     * Sem rede, cai no cache. Sem cache e sem rede, lança IOException.
     */
    suspend fun carregar(forcar: Boolean = false): Resultado = withContext(Dispatchers.IO) {
        // só debug: um catálogo de teste na pasta do app substitui o da rede (sem gravar cache)
        arquivoDeTeste()?.let { return@withContext ler(it.readText(), doCache = false) }
        val agora = System.currentTimeMillis()
        val cache = if (arquivo.exists()) arquivo.readText() else null
        val buscadoEm = prefs.getLong(CHAVE_BUSCA, 0L)
        if (!forcar && cache != null && Validade.fresco(buscadoEm, agora, Validade.SEIS_HORAS)) {
            try {
                return@withContext ler(cache, doCache = true)
            } catch (_: JSONException) {
                // cache quebrado: busca de novo abaixo
            }
        }
        try {
            val resposta = Rede.texto(URL)
            if (resposta.codigo !in 200..299) throw IOException("HTTP ${resposta.codigo}")
            val resultado = ler(resposta.corpo, doCache = false)
            arquivo.writeText(resposta.corpo)
            prefs.edit().putLong(CHAVE_BUSCA, agora).apply()
            resultado
        } catch (e: IOException) {
            recorrerAoCache(cache, e)
        } catch (e: JSONException) {
            recorrerAoCache(cache, e)
        }
    }

    /** `ports-catalogue.json` na pasta externa do app, só em build de debug. */
    private fun arquivoDeTeste(): File? =
        if (!BuildConfig.DEBUG) null else contexto.getExternalFilesDir(null)?.resolve("ports-catalogue.json")?.takeIf { it.exists() }

    private fun recorrerAoCache(cache: String?, erro: Exception): Resultado {
        if (cache == null) throw erro
        return ler(cache, doCache = true)
    }

    private fun ler(texto: String, doCache: Boolean): Resultado {
        val raiz = JSONObject(texto)
        val ports = raiz.getJSONArray("ports").paraObjetos().mapNotNull { o ->
            // entrada torta não derruba o catálogo inteiro
            try {
                lerPort(o)
            } catch (_: JSONException) {
                null
            }
        }
        return Resultado(ports, raiz.optTexto("updatedAt"), doCache)
    }

    private fun lerPort(o: JSONObject): Port? {
        val android = o.optJSONObject("assets")?.optJSONObject("android") ?: return null
        val id = o.getString("id")
        val titulo = o.getString("title")
        val repo = o.getString("repo")
        val notas = textoIdioma(o.optJSONObject("game")?.optJSONObject("notes"))
            ?: textoIdioma(o.optJSONObject("setupHint"))
        return Port(
            id = id,
            titulo = titulo,
            logo = Titulo.logo(titulo),
            nomePort = repo.substringAfter('/'),
            tipo = "",
            // sem plataforma, `other` ou `unknown` viram Outros (Plataformas.nome)
            plataforma = Plataformas.nome(o.optString("platform")),
            repo = repo,
            descricao = textoIdioma(o.optJSONObject("description")) ?: "",
            arquivoQueServe = notas ?: "",
            tamanho = "",
            versaoMaisNova = null,
            pacote = null,
            cores = Paleta.de(id),
            capaUrl = o.optTexto("coverImageUrl"),
            heroUrl = o.optTexto("heroImageUrl"),
            padraoApk = android.optTexto("pattern"),
            canal = o.optTexto("channel") ?: "latest",
            estrelas = o.optJSONObject("stats")?.takeIf { !it.isNull("stars") }?.optInt("stars"),
            aiFlag = o.optBoolean("aiFlag", false),
        )
    }

    /** Texto em pt-BR se houver, senão em inglês. */
    private fun textoIdioma(obj: JSONObject?): String? =
        obj?.optTexto("pt-BR") ?: obj?.optTexto("en")

    private companion object {
        const val URL = "https://raw.githubusercontent.com/victormoraesaraujo343/ports-catalogue/main/ports-catalogue.json"
        const val CHAVE_BUSCA = "buscadoEm"
    }
}
