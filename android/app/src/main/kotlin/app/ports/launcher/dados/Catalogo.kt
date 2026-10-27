package app.ports.launcher.dados

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.ports.core.Arquivo
import app.ports.core.Estado
import app.ports.core.Fatos
import app.ports.core.Ordem
import app.ports.core.Port
import app.ports.core.PortNaTela
import app.ports.core.Prontidao
import org.json.JSONArray
import org.json.JSONObject

/**
 * O catálogo do esqueleto: `assets/catalogo-exemplo.json`, gerado dos dados do
 * lab. O catálogo de verdade (ports-catalogue + `assets.android`) é o roadmap 4.
 */
class Catalogo(private val contexto: Context) {
    private val json by lazy { JSONObject(contexto.assets.open("catalogo-exemplo.json").bufferedReader().readText()) }

    val ports: List<Port> by lazy { json.getJSONArray("ports").objetos().map(::lerPort) }
    private val idsRecentes: List<String> by lazy { json.getJSONArray("recentes").textos() }

    fun naTela(): List<PortNaTela> = ports.map(::calcular).sortedWith(Ordem.alfabetica)
    fun recentes(): List<PortNaTela> = idsRecentes.mapNotNull { id -> ports.find { it.id == id } }.map(::calcular)
    fun porId(id: String): PortNaTela? = ports.find { it.id == id }?.let(::calcular)

    /**
     * Se o port tem pacote e ele está instalado no aparelho, o estado vem dos
     * fatos de verdade. Senão, no esqueleto, vale o estado de exemplo do lab.
     */
    private fun calcular(p: Port): PortNaTela {
        val instalada = p.pacote?.let(::versaoInstalada)
        if (instalada != null) {
            val fatos = Fatos(instalado = true, versaoInstalada = instalada, versaoMaisNova = p.versaoMaisNova, arquivo = Arquivo.CONFERIDO, rodaAqui = p.rodaAqui)
            return PortNaTela(p, Prontidao.calcular(fatos), instalada)
        }
        return PortNaTela(p, p.estadoDeExemplo ?: Estado.DISPONIVEL, versaoDeExemplo(p), p.progressoDeExemplo)
    }

    private fun versaoDeExemplo(p: Port) = json.getJSONArray("ports").objetos().find { it.getString("id") == p.id }?.optStringOuNull("versaoInstalada")

    private fun versaoInstalada(pacote: String): String? = try {
        contexto.packageManager.getPackageInfo(pacote, 0).versionName ?: "?"
    } catch (_: PackageManager.NameNotFoundException) { null }

    /** Abre o port, se ele estiver instalado. Volta false quando não dá. */
    fun abrir(p: Port): Boolean {
        val intent: Intent = p.pacote?.let { contexto.packageManager.getLaunchIntentForPackage(it) } ?: return false
        contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    private fun lerPort(o: JSONObject) = Port(
        id = o.getString("id"),
        titulo = o.getString("titulo"),
        logo = o.getString("logo"),
        nomePort = o.getString("port"),
        tipo = o.getString("tipo"),
        plataforma = o.getString("plataforma"),
        repo = o.getString("repo"),
        descricao = o.getString("descricao"),
        arquivoQueServe = o.getString("arquivo"),
        tamanho = o.getString("tamanho"),
        versaoMaisNova = o.optStringOuNull("versaoMaisNova"),
        pacote = o.optStringOuNull("pacote"),
        cores = o.optJSONArray("cores")?.textos()?.map { 0xFF000000 or it.removePrefix("#").toLong(16) },
        rodaAqui = o.optBoolean("rodaAqui", true),
        motivoNaoRoda = o.optStringOuNull("motivo"),
        estadoDeExemplo = o.optStringOuNull("estadoDeExemplo")?.let(Estado::valueOf),
        progressoDeExemplo = if (o.isNull("progresso")) null else o.optInt("progresso"),
        jogadoHa = o.optStringOuNull("jogadoHa"),
    )
}

private fun JSONArray.objetos() = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.textos() = (0 until length()).map { getString(it) }
private fun JSONObject.optStringOuNull(k: String): String? = if (isNull(k) || !has(k)) null else getString(k)
