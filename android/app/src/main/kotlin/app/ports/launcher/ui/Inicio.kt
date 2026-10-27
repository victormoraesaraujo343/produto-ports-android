package app.ports.launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Estado
import app.ports.core.PortNaTela
import app.ports.core.Textos
import app.ports.core.Titulo

/**
 * INÍCIO — deitado. O jogo em foco é o fundo da tela (aprovado na R1); a
 * fileira é atalho pros recentes (ou, sem recentes, os mais estrelados); pra
 * baixo abre a lista completa (Q-c). A joga, Y abre os detalhes.
 */
@Composable
fun Inicio(
    itens: List<PortNaTela>,
    focoInicial: String?,
    aoFocar: (PortNaTela?) -> Unit,
    aoAcao: (PortNaTela) -> Unit,
    aoDetalhes: (PortNaTela) -> Unit,
    aoTodos: () -> Unit,
    aoSecao: (Secao) -> Unit,
) {
    // o foco guarda o id, não o objeto: a lista muda quando chegam capas e estados
    var focadoId by remember { mutableStateOf(focoInicial) }
    val focado = itens.find { it.port.id == focadoId } ?: itens.firstOrNull()
    var noTodos by remember { mutableStateOf(false) }
    val entrada = LocalEntrada.current.atual
    val pedirCapa = LocalPedirCapa.current

    Fundo {
        // arte do jogo em foco, trocando com um fade curto (P-19/P-20)
        Crossfade(targetState = if (noTodos) null else focado?.port?.id, animationSpec = tween(220), label = "fundo") { id ->
            val pt = itens.find { it.port.id == id }
            // sem fundo oficial, a capa entra borrada (não é arte de tela cheia)
            if (pt != null) Arte(pt.port, Modifier.fillMaxSize(), comNome = false, url = pt.port.heroUrl ?: pt.port.capaUrl, larguraMax = 960, desfocar = pt.port.heroUrl == null)
            else Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF2A2A2A), Color(0xFF161616), Color(0xFF101010)))))
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.26f to Color(0xF80E0E0E), 0.56f to Color(0x9E0E0E0E), 1f to Color(0x260E0E0E))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.54f to Color.Transparent, 0.94f to Color(0xF50E0E0E))))

        Topo(Secao.INICIO, aoSecao)

        // informações do jogo em foco
        Column(Modifier.padding(start = 32.dp, top = 54.dp).width(460.dp)) {
            val pt = if (noTodos) null else focado
            if (pt == null) {
                Texto("TODOS OS PORTS", Tipo.logo, Modifier.padding(top = 0.dp))
                Row(Modifier.padding(top = 14.dp)) { BotaoHydra("Abrir a lista", true, Botao.A, aoClicar = aoTodos) }
            } else {
                val t = Textos.de(pt.estado)
                Texto(Titulo.semParenteses(pt.port.titulo), Tipo.logo, Modifier.padding(top = 0.dp))
                if (pt.estado != Estado.PRONTO) Box(Modifier.padding(top = 9.dp)) { LinhaEstado(pt) }
                if (pt.estado == Estado.BAIXANDO) Barra(pt.progresso ?: 0, Modifier.padding(top = 8.dp).width(220.dp))
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoHydra(Textos.acaoNaLista(pt.estado), true, Botao.A, aoClicar = { aoAcao(pt) })
                    if (entrada == Entrada.TOQUE) BotaoHydra("⋯", false, null, aoClicar = { aoDetalhes(pt) })
                }
            }
        }

        // fileira de recentes (ou dos mais estrelados) e o atalho pra lista toda
        val primeiro = remember { FocusRequester() }
        LazyRow(
            Modifier.align(Alignment.BottomStart).padding(bottom = 36.dp).fillMaxWidth()
                .onPreviewKeyEvent { e ->
                    // ↓ abre a lista completa (decisão Q-c)
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) { aoTodos(); true } else false
                },
            contentPadding = PaddingValues(horizontal = 27.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            items(itens, key = { it.port.id }) { pt ->
                LaunchedEffect(pt.port.id) { pedirCapa(pt.port.id) }
                var temFoco by remember { mutableStateOf(false) }
                val escala by animateFloatAsState(if (temFoco) 1.1f else 1f, tween(120), label = "escala")
                Box(
                    Modifier
                        .then(if (pt.port.id == focado?.port?.id) Modifier.focusRequester(primeiro) else Modifier)
                        .onFocusChanged {
                            temFoco = it.isFocused
                            if (it.isFocused) { focadoId = pt.port.id; noTodos = false; aoFocar(pt) }
                        }
                        .scale(escala)
                        .contornoFoco(temFoco, entrada = entrada)
                        .size(132.dp, 74.dp)
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .acionavel { aoAcao(pt) }
                ) {
                    Arte(pt.port, Modifier.fillMaxSize(), tamanhoNome = 12.sp, url = pt.port.capaUrl, larguraMax = 320)
                    Selo(pt, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                    if (pt.estado == Estado.BAIXANDO) Barra(pt.progresso ?: 0, Modifier.align(Alignment.BottomStart).fillMaxWidth())
                }
            }
            item(key = "todos") {
                var temFoco by remember { mutableStateOf(false) }
                Box(
                    Modifier.onFocusChanged { temFoco = it.isFocused; if (it.isFocused) { noTodos = true; focadoId = null; aoFocar(null) } }
                        .contornoFoco(temFoco, entrada = entrada)
                        .size(132.dp, 74.dp)
                        .border(1.dp, Cor.bordaForte, RoundedCornerShape(6.dp))
                        .acionavel { aoTodos() },
                    contentAlignment = Alignment.Center,
                ) {
                    Texto("Todos os ports", Tipo.botao.copy(fontSize = 12.5.sp, color = Cor.texto))
                }
            }
        }
        LaunchedEffect(Unit) { primeiro.focarQuandoPuder() }

        Dicas(
            if (noTodos || focado == null || Textos.naoInstalado(focado!!.estado)) emptyList() else listOf(Botao.Y to "Detalhes"),
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 10.dp),
        )
    }
}
