package app.ports.launcher.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Estado
import app.ports.core.Filtro
import app.ports.core.PortNaTela
import app.ports.core.Textos

/**
 * PORTS — deitado. Todos os ports em A–Z (ordem estável), filtros pelo que o
 * app já sabe, capas verticais (aprovado na R1). O port em foco aparece na
 * faixa de baixo: contexto na seleção, sem abrir outra tela.
 */
@Composable
fun Ports(
    todos: List<PortNaTela>,
    focoInicial: String?,
    aoFocar: (PortNaTela?) -> Unit,
    aoAcao: (PortNaTela) -> Unit,
    aoSecao: (Secao) -> Unit,
) {
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }
    var focado by remember { mutableStateOf<PortNaTela?>(null) }
    val entrada = LocalEntrada.current.atual
    val lista = todos.filter(filtro.aceita)

    Fundo {
        Topo(Secao.PORTS, aoSecao)

        // filtros
        Row(Modifier.padding(start = 21.dp, end = 26.dp, top = 46.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Filtro.entries.forEach { f ->
                var temFoco by remember { mutableStateOf(false) }
                val sel = f == filtro
                Row(
                    Modifier.onFocusChanged { temFoco = it.isFocused; if (it.isFocused) aoFocar(null) }
                        .contornoFoco(temFoco, raio = Raio, entrada = entrada)
                        .height(32.dp)
                        .clip(RoundedCornerShape(Raio))
                        .then(if (sel) Modifier.background(Cor.claro) else Modifier.border(1.dp, Cor.borda, RoundedCornerShape(Raio)))
                        .acionavel { filtro = f }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Texto(f.nome, Tipo.secao.copy(fontSize = 13.sp, color = if (sel) Color(0xFF121212) else Cor.texto))
                    Texto("${todos.count(f.aceita)}", Tipo.dica.copy(fontSize = 12.sp, color = if (sel) Color(0xFF4A4B52) else Cor.texto2))
                }
            }
            Box(Modifier.weight(1f))
            Texto("Ordem: A–Z", Tipo.dica)
        }

        // grade de capas verticais
        val primeiro = remember { FocusRequester() }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(108.dp),
            modifier = Modifier.padding(top = 92.dp, bottom = 52.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 6.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(lista, key = { it.port.id }) { pt ->
                var temFoco by remember { mutableStateOf(false) }
                val sobe by animateDpAsState(if (temFoco) (-3).dp else 0.dp, tween(120), label = "sobe")
                Box(
                    Modifier
                        .then(if (pt.port.id == (focoInicial ?: lista.firstOrNull()?.port?.id)) Modifier.focusRequester(primeiro) else Modifier)
                        .offset(y = sobe)
                        .onFocusChanged { temFoco = it.isFocused; if (it.isFocused) { focado = pt; aoFocar(pt) } }
                        .contornoFoco(temFoco, entrada = entrada)
                        .aspectRatio(3f / 4f)
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .acionavel { aoAcao(pt) },
                ) {
                    Arte(pt.port, Modifier.fillMaxSize())
                    Selo(pt, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                    if (pt.estado == Estado.BAIXANDO) Barra(pt.progresso ?: 0, Modifier.align(Alignment.BottomStart).fillMaxWidth())
                }
            }
        }
        LaunchedEffect(filtro) { primeiro.focarQuandoPuder() }

        // faixa de contexto: o port em foco
        if (entrada != Entrada.TOQUE) Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(52.dp).background(Cor.fundoEscuro).padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val pt = focado
            if (pt != null) {
                Column {
                    Texto(pt.port.titulo, Tipo.botao.copy(color = Cor.claro))
                    Texto("${pt.port.nomePort} · ${pt.port.plataforma}", Tipo.dica.copy(fontSize = 12.sp))
                }
                LinhaEstado(pt)
            }
            Box(Modifier.weight(1f))
            Dicas(if (pt != null) listOf(Botao.A to Textos.de(pt.estado).acao, Botao.Y to "Detalhes") else listOf(Botao.A to "Escolher", Botao.B to "Voltar"))
        }
    }
}
