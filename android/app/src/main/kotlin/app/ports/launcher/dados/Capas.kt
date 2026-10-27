package app.ports.launcher.dados

import android.content.Context
import app.ports.core.Titulo
import app.ports.core.Validade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder

/**
 * Capa e fundo do jogo no SteamGridDB (mesma lógica do fork do Hydra). Sem
 * chave, nada é buscado. Acertos ficam pra sempre; faltas, 7 dias.
 */
class Capas(contexto: Context) {
    private class ChaveRecusada : Exception()

    private val ajustes = contexto.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    private val arquivo = File(contexto.filesDir, "capas.json")
    private val limite = Semaphore(3)
    private val lock = Any()
    private val cache: JSONObject by lazy { carregarCache() }

    @Volatile private var chaveRecusada = false

    /** Vira true quando o SteamGridDB recusa a chave (401/403). */
    val chaveInvalida = MutableStateFlow(false)

    var chave: String
        get() = ajustes.getString(CHAVE, "").orEmpty()
        set(valor) {
            ajustes.edit().putString(CHAVE, valor.trim()).apply()
            chaveRecusada = false
            chaveInvalida.value = false
        }

    /** (capa, fundo). Pares nulos quando não há chave, não acha ou a rede caiu. */
    suspend fun buscar(titulo: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val key = chave.trim()
        val busca = Titulo.limparParaBusca(titulo)
        if (key.isEmpty() || busca.isEmpty() || chaveRecusada) return@withContext NADA
        doCache(busca)?.let { return@withContext it }
        limite.withPermit { consultar(busca, key) }
    }

    /** Resposta do cache, ou nulo se é preciso consultar de novo. */
    private fun doCache(busca: String): Pair<String?, String?>? {
        synchronized(lock) {
            val o = cache.optJSONObject(busca) ?: return null
            if (o.optBoolean("achou")) return o.optTexto("capa") to o.optTexto("heroi")
            if (Validade.fresco(o.optLong("buscadoEm"), System.currentTimeMillis(), Validade.SETE_DIAS)) return NADA
            return null
        }
    }

    private fun consultar(busca: String, key: String): Pair<String?, String?> {
        return try {
            val id = procurarId(busca, key)
            val resultado = if (id == null) NADA else {
                primeiraUrl("grids/game/$id?dimensions=600x900&types=static", key) to
                    primeiraUrl("heroes/game/$id", key)
            }
            gravar(busca, resultado.first, resultado.second, achou = resultado.first != null || resultado.second != null)
            resultado
        } catch (_: ChaveRecusada) {
            chaveRecusada = true
            chaveInvalida.value = true
            NADA
        } catch (_: IOException) {
            // sem rede agora: não grava, tenta depois
            NADA
        }
    }

    private fun procurarId(busca: String, key: String): Int? {
        val termo = URLEncoder.encode(busca, "UTF-8").replace("+", "%20")
        val json = pedir("search/autocomplete/$termo", key) ?: return null
        return json.optJSONArray("data")?.paraObjetos()?.firstOrNull()?.optInt("id", -1)?.takeIf { it > 0 }
    }

    private fun primeiraUrl(caminho: String, key: String): String? =
        pedir(caminho, key)?.optJSONArray("data")?.paraObjetos()?.firstOrNull()?.optTexto("url")

    private fun pedir(caminho: String, key: String): JSONObject? {
        val resposta = Rede.texto("$BASE/$caminho", mapOf("Authorization" to "Bearer $key"))
        return when (resposta.codigo) {
            in 200..299 -> try {
                JSONObject(resposta.corpo)
            } catch (_: JSONException) {
                null
            }
            401, 403 -> throw ChaveRecusada()
            404 -> null
            else -> throw IOException("SteamGridDB respondeu ${resposta.codigo}")
        }
    }

    private fun gravar(busca: String, capa: String?, heroi: String?, achou: Boolean) {
        synchronized(lock) {
            cache.put(
                busca,
                JSONObject()
                    .put("capa", capa ?: JSONObject.NULL)
                    .put("heroi", heroi ?: JSONObject.NULL)
                    .put("achou", achou)
                    .put("buscadoEm", System.currentTimeMillis()),
            )
            arquivo.writeText(cache.toString())
        }
    }

    private fun carregarCache(): JSONObject = try {
        JSONObject(arquivo.readText())
    } catch (_: Exception) {
        JSONObject()
    }

    private companion object {
        const val CHAVE = "steamgriddb"
        const val BASE = "https://www.steamgriddb.com/api/v2"
        val NADA: Pair<String?, String?> = null to null
    }
}
