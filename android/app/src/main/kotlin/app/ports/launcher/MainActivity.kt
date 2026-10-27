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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.ports.core.Estado
import app.ports.core.PortNaTela
import app.ports.launcher.dados.Catalogo
import app.ports.launcher.ui.Cor
import app.ports.launcher.ui.Detalhes
import app.ports.launcher.ui.Entrada
import app.ports.launcher.ui.EstadoEntrada
import app.ports.launcher.ui.Fundo
import app.ports.launcher.ui.Inicio
import app.ports.launcher.ui.LocalEntrada
import app.ports.launcher.ui.Ports
import app.ports.launcher.ui.Secao
import app.ports.launcher.ui.Texto
import app.ports.launcher.ui.Tipo
import app.ports.launcher.ui.Topo

/** Pra onde se navega. A pilha só cresce em telas de dentro (Detalhes). */
sealed interface Tela {
    data class DeSecao(val secao: Secao) : Tela
    data class DeDetalhes(val id: String) : Tela
}

class MainActivity : ComponentActivity() {
    private val entrada = EstadoEntrada()
    private val atalhos = Atalhos()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // tela cheia de verdade: esconde as barras do sistema; aparecem só com swipe
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        val catalogo = Catalogo(this)

        setContent {
            CompositionLocalProvider(LocalEntrada provides entrada) {
                val pilha = remember { mutableStateListOf<Tela>(Tela.DeSecao(Secao.INICIO)) }
                var focado by remember { mutableStateOf<PortNaTela?>(null) }
                val tela = pilha.last()

                fun irSecao(s: Secao) { pilha.clear(); pilha.add(Tela.DeSecao(s)); focado = null }
                fun detalhes(pt: PortNaTela) = pilha.add(Tela.DeDetalhes(pt.port.id))
                fun acao(pt: PortNaTela) {
                    when (pt.estado) {
                        Estado.PRONTO, Estado.ATUALIZAR, Estado.BAIXANDO ->
                            if (!catalogo.abrir(pt.port)) aviso("Abrir ${pt.port.titulo}: o catálogo de exemplo ainda não tem o pacote Android desse port.")
                        Estado.NAO_RODA -> detalhes(pt)
                        else -> aviso("“${app.ports.core.Textos.de(pt.estado).acao}” ainda não está no app. Vem nas próximas issues.")
                    }
                }

                // L1/R1 trocam de seção; Y abre detalhes do jogo em foco
                atalhos.l1 = { (tela as? Tela.DeSecao)?.let { irSecao(Secao.entries[(it.secao.ordinal + 2) % 3]) } }
                atalhos.r1 = { (tela as? Tela.DeSecao)?.let { irSecao(Secao.entries[(it.secao.ordinal + 1) % 3]) } }
                atalhos.y = { if (tela is Tela.DeSecao) focado?.let(::detalhes) }

                BackHandler(enabled = pilha.size > 1 || tela != Tela.DeSecao(Secao.INICIO)) {
                    if (pilha.size > 1) pilha.removeAt(pilha.lastIndex) else irSecao(Secao.INICIO)
                }

                Box(Modifier.fillMaxSize()) {
                    when (tela) {
                        is Tela.DeSecao -> when (tela.secao) {
                            Secao.INICIO -> Inicio(
                                recentes = catalogo.recentes(), total = catalogo.ports.size, focoInicial = focado?.port?.id,
                                aoFocar = { focado = it }, aoAcao = ::acao, aoDetalhes = ::detalhes,
                                aoTodos = { irSecao(Secao.PORTS) }, aoSecao = ::irSecao,
                            )
                            Secao.PORTS -> Ports(catalogo.naTela(), focado?.port?.id, aoFocar = { focado = it }, aoAcao = ::acao, aoSecao = ::irSecao)
                            Secao.AJUSTES -> Fundo {
                                Topo(Secao.AJUSTES, ::irSecao)
                                Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                                    Texto("Ajustes chegam numa próxima issue.", Tipo.corpo.copy(color = Cor.claro))
                                    Texto("O desenho está no lab: laboratorio/ajustes.", Tipo.dica)
                                }
                            }
                        }
                        is Tela.DeDetalhes -> catalogo.porId(tela.id)?.let { Detalhes(it, aoAcao = ::acao, aoVoltar = { onBackPressedDispatcher.onBackPressed() }) }
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

    private fun aviso(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()

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
