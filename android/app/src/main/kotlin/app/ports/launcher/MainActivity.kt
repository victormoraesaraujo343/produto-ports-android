package app.ports.launcher

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import app.ports.core.Estado
import app.ports.core.PortNaTela
import app.ports.core.Textos
import app.ports.core.Titulo
import app.ports.core.Vr
import app.ports.launcher.dados.EstadoCatalogo
import app.ports.launcher.dados.Repositorio
import app.ports.launcher.dados.ResultadoInstalacao
import app.ports.launcher.ui.Ajustes
import app.ports.launcher.ui.Detalhes
import app.ports.launcher.ui.Entrada
import app.ports.launcher.ui.EstadoEntrada
import app.ports.launcher.ui.Inicio
import app.ports.launcher.ui.LocalEntrada
import app.ports.launcher.ui.LocalPedirCapa
import app.ports.launcher.ui.LocalRepositorio
import app.ports.launcher.ui.Ports
import app.ports.launcher.ui.Secao
import app.ports.launcher.ui.TelaSemLista
import kotlinx.coroutines.launch

/** Pra onde se navega. A pilha só cresce em telas de dentro (Detalhes). */
sealed interface Tela {
    data class DeSecao(val secao: Secao) : Tela
    data class DeDetalhes(val id: String) : Tela
}

class MainActivity : ComponentActivity() {
    private val entrada = EstadoEntrada()
    private val atalhos = Atalhos()
    private lateinit var repositorio: Repositorio

    /** Ports cuja capa já foi pedida nesta sessão (limpa quando a chave muda). */
    private val capasPedidas = HashSet<String>()

    /** Sobe a cada volta ao app: um jogo aberto pode ter mudado a fileira de recentes. */
    private var voltas by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // tela cheia de verdade: esconde as barras do sistema; aparecem só com swipe
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        repositorio = Repositorio.obter(applicationContext)

        setContent {
            CompositionLocalProvider(
                LocalEntrada provides entrada,
                LocalRepositorio provides repositorio,
                LocalPedirCapa provides pedirCapaAoAparecer,
            ) {
                val estado by repositorio.estado.collectAsState()
                val pilha = remember { mutableStateListOf<Tela>(Tela.DeSecao(Secao.INICIO)) }
                var focadoId by remember { mutableStateOf<String?>(null) }
                // o console escolhido fica enquanto o app está aberto (passa por todas as seções)
                var console by rememberSaveable { mutableStateOf<String?>(null) }
                val tela = pilha.last()

                LaunchedEffect(Unit) { repositorio.atualizar() }
                LaunchedEffect(Unit) { repositorio.avisos.collect { aviso(it) } }

                val pronto = estado as? EstadoCatalogo.Pronto
                // um único filtro: ports de VR saem de todas as listas
                val visiveis = remember(pronto) { pronto?.ports.orEmpty().filter { !Vr.ehVr(it.port.titulo) } }
                val recentes = remember(visiveis, voltas) { repositorio.recentes().filter { !Vr.ehVr(it.port.titulo) } }
                // sem recentes, a fileira mostra os mais estrelados
                val fileira = if (recentes.isNotEmpty()) recentes else visiveis.sortedByDescending { it.port.estrelas ?: -1 }.take(8)

                fun irSecao(s: Secao) { pilha.clear(); pilha.add(Tela.DeSecao(s)); focadoId = null }
                fun detalhes(id: String) { pilha.add(Tela.DeDetalhes(id)) }

                // L1/R1 trocam de seção; Y abre detalhes do jogo em foco
                atalhos.l1 = { (tela as? Tela.DeSecao)?.let { irSecao(Secao.entries[(it.secao.ordinal + 2) % 3]) } }
                atalhos.r1 = { (tela as? Tela.DeSecao)?.let { irSecao(Secao.entries[(it.secao.ordinal + 1) % 3]) } }
                atalhos.y = { if (tela is Tela.DeSecao) focadoId?.let { detalhes(it) } }

                BackHandler(enabled = pilha.size > 1 || tela != Tela.DeSecao(Secao.INICIO)) {
                    if (pilha.size > 1) pilha.removeAt(pilha.lastIndex) else irSecao(Secao.INICIO)
                }

                Box(Modifier.fillMaxSize()) {
                    when (tela) {
                        is Tela.DeSecao -> when (tela.secao) {
                            Secao.INICIO ->
                                if (pronto == null) TelaSemLista(Secao.INICIO, estado, { irSecao(it) }, { atualizarLista() })
                                else Inicio(
                                    itens = fileira, focoInicial = focadoId,
                                    aoFocar = { focadoId = it?.port?.id }, aoAcao = { if (Textos.naoInstalado(it.estado)) detalhes(it.port.id) else acao(it) }, aoDetalhes = { detalhes(it.port.id) },
                                    aoTodos = { irSecao(Secao.PORTS) }, aoSecao = { irSecao(it) },
                                )
                            Secao.PORTS ->
                                if (pronto == null) TelaSemLista(Secao.PORTS, estado, { irSecao(it) }, { atualizarLista() })
                                else Ports(
                                    visiveis, focadoId, aoFocar = { focadoId = it?.port?.id }, aoAcao = { if (Textos.naoInstalado(it.estado)) detalhes(it.port.id) else acao(it) }, aoSecao = { irSecao(it) },
                                    console = console, aoConsole = { console = it },
                                )
                            Secao.AJUSTES -> Ajustes(
                                atualizadoEm = pronto?.atualizadoEm,
                                aoSecao = { irSecao(it) },
                                aoChaveMudou = { capasPedidas.clear() },
                                aoAtualizarLista = { atualizarLista() },
                            )
                        }
                        is Tela.DeDetalhes -> {
                            val p = visiveis.find { it.port.id == tela.id }
                            if (p != null) Detalhes(
                                p,
                                aoAcao = { acao(p) },
                                aoAtualizar = { instalar(p.port.id) },
                                aoVoltar = { onBackPressedDispatcher.onBackPressed() },
                            )
                            else TelaSemLista(null, estado, { irSecao(it) }, { atualizarLista() })
                        }
                    }
                }
            }
        }
    }

    // o sistema volta a mostrar as barras quando a janela ganha foco de novo
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onResume() {
        super.onResume()
        voltas++
    }

    private fun aviso(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()

    /** Estado do port agora, não o da tela (que pode estar um quadro atrás). */
    private fun atual(id: String): PortNaTela? =
        (repositorio.estado.value as? EstadoCatalogo.Pronto)?.ports?.find { it.port.id == id }

    /** A ação principal: abre o que está instalado, instala o que não está. */
    private fun acao(pt: PortNaTela) {
        val p = atual(pt.port.id) ?: pt
        when (p.estado) {
            Estado.PRONTO, Estado.ATUALIZAR -> abrir(p)
            // baixando sem nada instalado não tem o que abrir: o progresso aparece na capa
            Estado.BAIXANDO -> if (p.versaoInstalada != null) abrir(p)
            Estado.DISPONIVEL, Estado.NAO_RODA -> instalar(p.port.id)
            else -> aviso("“${Textos.de(p.estado).acao}” ainda não está no app.")
        }
    }

    private fun abrir(p: PortNaTela) {
        if (!repositorio.abrir(p.port.id)) aviso("Não deu pra abrir ${Titulo.semParenteses(p.port.titulo)}.")
    }

    /** Baixa e entrega ao instalador do sistema. O resultado final chega pelo receptor. */
    private fun instalar(id: String) {
        lifecycleScope.launch {
            when (val r = repositorio.instalar(id)) {
                is ResultadoInstalacao.PrecisaPermissao -> {
                    startActivity(r.intent)
                    aviso("Libere a instalação de apps e tente de novo.")
                }
                is ResultadoInstalacao.Falhou -> aviso(r.mensagem)
                ResultadoInstalacao.Iniciado -> Unit
            }
        }
    }

    private fun atualizarLista() {
        lifecycleScope.launch { repositorio.atualizar(forcar = true) }
    }

    /** Cada capa é pedida uma vez: a fileira e a grade chamam isto ao aparecer. Uma instância só, pra não repintar à toa. */
    private val pedirCapaAoAparecer: (String) -> Unit = { id ->
        if (capasPedidas.add(id)) lifecycleScope.launch { repositorio.capas(id) }
    }

    /* ------------------------------------------------------------ controle
       A vira "confirmar" (o acionavel do Compose escuta DPAD_CENTER/Enter), B é
       voltar. Teclado: Enter/Esc, Q/E = L1/R1, Y = Y — pra testar no emulador. */
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val deControle = e.isFromSource(InputDevice.SOURCE_GAMEPAD) || e.isFromSource(InputDevice.SOURCE_JOYSTICK)
        if (e.action == KeyEvent.ACTION_DOWN) {
            entrada.atual = if (deControle && e.device?.vendorId == SONY) Entrada.PLAYSTATION else Entrada.XBOX
        }
        val atalho = when (e.keyCode) {
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q -> atalhos.l1
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_E -> atalhos.r1
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_Y -> atalhos.y
            else -> null
        }
        if (atalho != null) { if (e.action == KeyEvent.ACTION_UP) atalho(); return true }
        val trocado = when (e.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_ESCAPE -> KeyEvent.KEYCODE_BACK
            else -> null
        }
        if (trocado != null) return super.dispatchKeyEvent(KeyEvent(e.downTime, e.eventTime, e.action, trocado, e.repeatCount, e.metaState, e.deviceId, e.scanCode, e.flags, e.source))
        return super.dispatchKeyEvent(e)
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN && e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) entrada.atual = Entrada.TOQUE
        return super.dispatchTouchEvent(e)
    }

    private class Atalhos { var l1: () -> Unit = {}; var r1: () -> Unit = {}; var y: () -> Unit = {} }

    private companion object { const val SONY = 0x054C }
}
