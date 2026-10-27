package app.ports.launcher.dados

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import app.ports.core.Arquivo
import app.ports.core.Estado
import app.ports.core.Fatos
import app.ports.core.Ordem
import app.ports.core.Port
import app.ports.core.PortNaTela
import app.ports.core.Prontidao
import app.ports.core.Tamanhos
import app.ports.core.Versoes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

/**
 * A porta de entrada da tela: catálogo, estado de cada port, instalação,
 * abertura e capas. Uma instância por processo (`Repositorio.obter`).
 */
class Repositorio(contexto: Context) {
    private val app = contexto.applicationContext
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val online = CatalogoOnline(app)
    private val releases = GitHubReleases(app)
    private val pacotes = Pacotes(app)
    private val instalador = Instalador(app)
    private val capasSgdb = Capas(app)
    private val uso = app.getSharedPreferences("uso", Context.MODE_PRIVATE)

    /** Carregador de imagens das capas (a UI usa `imagens.carregar(url, largura)`). */
    val imagens = Imagens(app)

    private val _estado = MutableStateFlow<EstadoCatalogo>(EstadoCatalogo.Carregando)
    val estado: StateFlow<EstadoCatalogo> = _estado.asStateFlow()

    /** Avisos que não cabem numa chamada: falha de instalação, chave do SteamGridDB recusada. */
    private val _avisos = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val avisos: SharedFlow<String> = _avisos.asSharedFlow()

    private val atualizacao = Mutex()
    private val lock = Any()
    private var base: List<Port> = emptyList()
    private var atualizadoEm: String? = null
    private var doCache = false
    private val extras = HashMap<String, Extras>()
    private val baixando = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val ultimoEnvio = HashMap<String, Long>()

    /** O que se descobre depois do catálogo: versão nova, tamanho, capas do SteamGridDB. */
    private data class Extras(val versao: String? = null, val tamanho: String = "", val capa: String? = null, val heroi: String? = null)

    init {
        escopo.launch {
            capasSgdb.chaveInvalida.collect { if (it) _avisos.emit("A chave do SteamGridDB não foi aceita. As capas ficam de fora.") }
        }
    }

    companion object {
        @Volatile private var instancia: Repositorio? = null

        fun obter(contexto: Context): Repositorio =
            instancia ?: synchronized(this) { instancia ?: Repositorio(contexto).also { instancia = it } }
    }

    /** Chave pessoal do SteamGridDB (opcional). Vazia: sem capas do SteamGridDB. */
    var chaveSteamGridDb: String
        get() = capasSgdb.chave
        set(valor) {
            capasSgdb.chave = valor
        }

    /** Busca o catálogo. Com `forcar` falso, o cache de até 6 h é usado. */
    suspend fun atualizar(forcar: Boolean = false) {
        withContext(Dispatchers.IO) {
            atualizacao.withLock {
                if (_estado.value !is EstadoCatalogo.Pronto) _estado.value = EstadoCatalogo.Carregando
                try {
                    val r = online.carregar(forcar)
                    synchronized(lock) {
                        base = r.ports
                        atualizadoEm = r.atualizadoEm
                        doCache = r.doCache
                    }
                    republicar()
                    escopo.launch { buscarVersoesDosInstalados() }
                } catch (_: IOException) {
                    if (_estado.value !is EstadoCatalogo.Pronto) {
                        _estado.value = EstadoCatalogo.Erro("Não deu pra baixar o catálogo. Confira a internet.")
                    }
                } catch (_: JSONException) {
                    if (_estado.value !is EstadoCatalogo.Pronto) {
                        _estado.value = EstadoCatalogo.Erro("O catálogo chegou num formato que o app não entende.")
                    }
                }
            }
        }
    }

    /** Os ports instalados ganham a versão mais nova (pra mostrar "Atualização"). */
    private suspend fun buscarVersoesDosInstalados() {
        val lista = synchronized(lock) { base.toList() }
        for (p in lista) {
            if (versaoInstaladaDe(p) != null) buscarLancamento(p)
        }
    }

    /** Ports que o jogador pode ver na lista de instalados, mais recente primeiro. */
    fun recentes(): List<PortNaTela> {
        val pronto = _estado.value as? EstadoCatalogo.Pronto ?: return emptyList()
        return pronto.ports
            .filter { it.versaoInstalada != null }
            .sortedByDescending { uso.getLong(it.port.id, 0L) }
    }

    /** Busca a release (versão e tamanho) e as capas do port, e atualiza o estado. */
    suspend fun detalhar(id: String) {
        withContext(Dispatchers.IO) {
            val p = synchronized(lock) { base.find { it.id == id } } ?: return@withContext
            buscarLancamento(p)
            capas(id)
        }
    }

    /** Baixa o APK da última versão e entrega ao instalador do sistema. */
    suspend fun instalar(id: String): ResultadoInstalacao = escopo.async { instalarAgora(id) }.await()

    /** Abre o port instalado e marca o horário de uso. Falso se não dá pra abrir. */
    fun abrir(id: String): Boolean {
        val pacote = synchronized(lock) { base.find { it.id == id } }?.let { pacotes.pacoteDe(it.repo) } ?: return false
        val intent = app.packageManager.getLaunchIntentForPackage(pacote) ?: return false
        return try {
            app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            uso.edit().putLong(id, System.currentTimeMillis()).apply()
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** (capa, fundo) do port. Usa o que veio do catálogo; o que falta vem do SteamGridDB. */
    suspend fun capas(id: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val p = synchronized(lock) { base.find { it.id == id } } ?: return@withContext null to null
        val (capaAtual, heroiAtual) = synchronized(lock) {
            (p.capaUrl ?: extras[id]?.capa) to (p.heroUrl ?: extras[id]?.heroi)
        }
        if (capaAtual != null && heroiAtual != null) return@withContext capaAtual to heroiAtual
        val (capa, heroi) = capasSgdb.buscar(p.titulo)
        synchronized(lock) {
            val atual = extras[id] ?: Extras()
            extras[id] = atual.copy(capa = atual.capa ?: capa, heroi = atual.heroi ?: heroi)
        }
        republicar()
        (capaAtual ?: capa) to (heroiAtual ?: heroi)
    }

    /* ------------------------------------------------------------ interno */

    private suspend fun instalarAgora(id: String): ResultadoInstalacao {
        val p = synchronized(lock) { base.find { it.id == id } }
            ?: return ResultadoInstalacao.Falhou("Esse port não está no catálogo.")
        if (!instalador.podeInstalar()) return ResultadoInstalacao.PrecisaPermissao(instalador.intentPermissao())
        if (baixando.value.containsKey(id)) return ResultadoInstalacao.Falhou("Esse port já está baixando.")
        val lancamento = releases.ultima(p.repo, p.canal, p.padraoApk)
            ?: return ResultadoInstalacao.Falhou("Não achei o APK da última versão. Tente de novo mais tarde.")
        marcarProgresso(id, 0)
        return try {
            val apk = instalador.baixar(id, lancamento.apkUrl) { marcarProgresso(id, it) }
            val pacote = instalador.lerPacote(apk) ?: run {
                removerProgresso(id)
                return ResultadoInstalacao.Falhou("O APK baixado não tem um pacote válido.")
            }
            pacotes.gravar(p.repo, pacote.nome)
            // 100% fica até o sistema responder (sucesso ou falha)
            marcarProgresso(id, 100)
            instalador.enviar(apk, pacote.nome, id)
            ResultadoInstalacao.Iniciado
        } catch (e: Exception) {
            // qualquer falha vira mensagem; nunca derruba o app
            removerProgresso(id)
            ResultadoInstalacao.Falhou("Não deu pra instalar: ${e.message ?: "erro desconhecido"}")
        }
    }

    /** Chamado pelo ReceptorInstalacao quando o sistema responde. */
    internal fun aoTerminarInstalacao(id: String, erro: String?) {
        escopo.launch {
            removerProgresso(id)
            if (erro == null) {
                uso.edit().putLong(id, System.currentTimeMillis()).apply()
            } else {
                _avisos.emit(erro)
            }
        }
    }

    private suspend fun buscarLancamento(p: Port): Release? {
        val rel = releases.ultima(p.repo, p.canal, p.padraoApk)
        synchronized(lock) {
            val atual = extras[p.id] ?: Extras()
            extras[p.id] = if (rel == null) atual else atual.copy(
                versao = Versoes.semV(rel.tag),
                tamanho = Tamanhos.formatar(rel.tamanhoBytes),
            )
        }
        republicar()
        return rel
    }

    /** Lança o progresso no máximo ~4 vezes por segundo (0 e 100 sempre passam). */
    private fun marcarProgresso(id: String, pct: Int) {
        val agora = System.currentTimeMillis()
        val envia = synchronized(lock) {
            val ok = pct <= 0 || pct >= 100 || agora - (ultimoEnvio[id] ?: 0L) >= 250
            if (ok) ultimoEnvio[id] = agora
            ok
        }
        if (!envia) return
        baixando.update { it + (id to pct) }
        republicar()
    }

    private fun removerProgresso(id: String) {
        baixando.update { it - id }
        synchronized(lock) { ultimoEnvio.remove(id) }
        republicar()
    }

    private fun versaoInstaladaDe(p: Port): String? = pacotes.pacoteDe(p.repo)?.let(pacotes::versaoInstalada)

    /** Recalcula a lista inteira e publica. Chamado com qualquer mudança de dado. */
    private fun republicar() {
        val progresso = baixando.value
        synchronized(lock) {
            val lista = base.map { montar(it, progresso[it.id]) }.sortedWith(Ordem.alfabetica)
            _estado.value = EstadoCatalogo.Pronto(lista, atualizadoEm, doCache)
        }
    }

    /** Estado de um port a partir dos fatos de verdade. Chamado com o lock. */
    private fun montar(p0: Port, progresso: Int?): PortNaTela {
        val ex = extras[p0.id]
        val pacote = pacotes.pacoteDe(p0.repo)
        val p = p0.copy(
            pacote = pacote,
            versaoMaisNova = ex?.versao ?: p0.versaoMaisNova,
            tamanho = ex?.tamanho ?: p0.tamanho,
            capaUrl = p0.capaUrl ?: ex?.capa,
            heroUrl = p0.heroUrl ?: ex?.heroi,
        )
        val instalada = pacote?.let(pacotes::versaoInstalada)
        val fatos = Fatos(
            instalado = instalada != null,
            versaoInstalada = instalada,
            versaoMaisNova = p.versaoMaisNova,
            arquivo = Arquivo.CONFERIDO,
            rodaAqui = p.rodaAqui,
            baixando = progresso,
        )
        // baixando vence "não instalado": o port precisa mostrar o progresso mesmo na primeira instalação
        val estado = if (progresso != null && p.rodaAqui) Estado.BAIXANDO else Prontidao.calcular(fatos)
        return PortNaTela(p, estado, instalada, progresso)
    }
}
