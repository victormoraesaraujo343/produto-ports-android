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

/**
 * INÍCIO — deitado. O jogo em foco é o fundo da tela (aprovado na R1); a
 * fileira é atalho pros recentes; pra baixo abre a lista completa (Q-c). O nome
 * aparece só na capa em foco (aprovado na R1). A joga, Y abre os detalhes.
 */
@Composable
fun Inicio(
    recentes: List<PortNaTela>,
    total: Int,
    focoInicial: String?,
    aoFocar: (PortNaTela?) -> Unit,
    aoAcao: (PortNaTela) -> Unit,
    aoDetalhes: (PortNaTela) -> Unit,
    aoTodos: () -> Unit,
    aoSecao: (Secao) -> Unit,
) {
    var focado by remember { mutableStateOf(recentes.find { it.port.id == focoInicial } ?: recentes.firstOrNull()) }
    var noTodos by remember { mutableStateOf(false) }
    val entrada = LocalEntrada.current.atual

    Fundo {
        // arte do jogo em foco, trocando com um fade curto (P-19/P-20)
        Crossfade(targetState = if (noTodos) null else focado, animationSpec = tween(220), label = "fundo") { pt ->
            if (pt != null) Arte(pt.port, Modifier.fillMaxSize(), comNome = false)
            else Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF2A2A2A), Color(0xFF161616), Color(0xFF101010)))))
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.26f to Color(0xF80E0E0E), 0.56f to Color(0x9E0E0E0E), 1f to Color(0x260E0E0E))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.54f to Color.Transparent, 0.94f to Color(0xF50E0E0E))))

        Topo(Secao.INICIO, aoSecao)

        // informações do jogo em foco
        Column(Modifier.padding(start = 32.dp, top = 54.dp).width(460.dp)) {
            val pt = focado
            if (noTodos || pt == null) {
                Texto("BIBLIOTECA", Tipo.sobre)
                Texto("TODOS OS PORTS", Tipo.logo, Modifier.padding(top = 2.dp))
                Texto("$total ports no catálogo", Tipo.corpo.copy(color = Color(0xFFE6E7EC), fontSize = 13.sp), Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 14.dp)) { BotaoHydra("Abrir a lista", true, Botao.A, aoClicar = aoTodos) }
            } else {
                val t = Textos.de(pt.estado)
                val sobre = when {
                    pt == recentes.firstOrNull() -> "CONTINUAR JOGANDO"
                    pt.port.jogadoHa != null && pt.port.jogadoHa != "nunca jogado" -> "JOGADO ${pt.port.jogadoHa!!.uppercase()}"
                    else -> "RECENTES"
                }
                Texto(sobre, Tipo.sobre)
                Texto(pt.port.logo, Tipo.logo, Modifier.padding(top = 2.dp))
                Texto("${pt.port.nomePort} · ${pt.port.tipo}${pt.versaoInstalada?.let { " · v$it" } ?: ""}", Tipo.corpo.copy(color = Color(0xFFE6E7EC), fontSize = 13.sp), Modifier.padding(top = 4.dp))
                Box(Modifier.padding(top = 9.dp)) { LinhaEstado(pt) }
                if (pt.estado == Estado.FALTA_ARQUIVO) Texto("Serve: ${pt.port.arquivoQueServe}. A gente confere o arquivo pra você.", Tipo.dica, Modifier.padding(top = 4.dp))
                if (pt.estado == Estado.BAIXANDO) Barra(pt.progresso ?: 0, Modifier.padding(top = 8.dp).width(220.dp))
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoHydra(t.acao, true, Botao.A, aoClicar = { aoAcao(pt) })
                    BotaoHydra("Detalhes", false, Botao.Y, aoClicar = { aoDetalhes(pt) })
                }
            }
        }

        // fileira de recentes
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
            items(recentes, key = { it.port.id }) { pt ->
                var temFoco by remember { mutableStateOf(false) }
                val escala by animateFloatAsState(if (temFoco) 1.1f else 1f, tween(120), label = "escala")
                Box(
                    Modifier
                        .then(if (pt == focado) Modifier.focusRequester(primeiro) else Modifier)
                        .onFocusChanged { temFoco = it.isFocused; if (it.isFocused) { focado = pt; noTodos = false; aoFocar(pt) } }
                        .scale(escala)
                        .contornoFoco(temFoco, entrada = entrada)
                        .size(132.dp, 74.dp)
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .acionavel { aoAcao(pt) }
                ) {
                    Arte(pt.port, Modifier.fillMaxSize(), tamanhoNome = 12.sp)
                    if (temFoco || entrada == Entrada.TOQUE) {
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000)))).padding(start = 8.dp, end = 32.dp, top = 16.dp, bottom = 6.dp)) {
                            Texto(pt.port.titulo, Tipo.botao.copy(fontSize = 12.sp, color = Color.White), linhas = 1)
                        }
                    }
                    Selo(pt, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                    if (pt.estado == Estado.BAIXANDO) Barra(pt.progresso ?: 0, Modifier.align(Alignment.BottomStart).fillMaxWidth())
                }
            }
            item(key = "todos") {
                var temFoco by remember { mutableStateOf(false) }
                Box(
                    Modifier.onFocusChanged { temFoco = it.isFocused; if (it.isFocused) { noTodos = true; aoFocar(null) } }
                        .contornoFoco(temFoco, entrada = entrada)
                        .size(132.dp, 74.dp)
                        .border(1.dp, Cor.bordaForte, RoundedCornerShape(6.dp))
                        .acionavel { aoTodos() },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Texto("Todos os ports", Tipo.botao.copy(fontSize = 12.5.sp, color = Cor.texto))
                        Texto("↓", Tipo.corpo)
                        Texto("$total no catálogo", Tipo.dica.copy(fontSize = 11.sp))
                    }
                }
            }
        }
        LaunchedEffect(Unit) { primeiro.focarQuandoPuder() }

        val pt = focado
        Dicas(
            if (noTodos || pt == null) listOf(Botao.A to "Abrir") else listOf(Botao.A to Textos.de(pt.estado).acao, Botao.Y to "Detalhes"),
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 10.dp),
        )
    }
}
