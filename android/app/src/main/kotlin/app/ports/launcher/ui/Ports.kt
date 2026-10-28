package app.ports.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Consoles
import app.ports.core.Estado
import app.ports.core.Filtro
import app.ports.core.JogoNaTela
import app.ports.core.PortNaTela
import app.ports.core.Textos
import app.ports.core.Titulo

/**
 * PORTS — deitado. Todos os ports em A–Z (ordem estável), filtros pelo que o
 * app já sabe, capas verticais (aprovado na R1). O port em foco aparece na
 * faixa de baixo: contexto na seleção, sem abrir outra tela.
 * À direita dos filtros, o console: abre um painel lateral com os consoles da lista.
 */
@Composable
fun Ports(
    jogos: List<JogoNaTela>,
    focoInicial: String?,
    aoFocar: (PortNaTela?) -> Unit,
    aoAcao: (PortNaTela) -> Unit,
    aoSecao: (Secao) -> Unit,
    console: String?,
    aoConsole: (String?) -> Unit,
) {
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }
    // o foco guarda a chave do jogo: a faixa de baixo segue o jogo mesmo quando a fonte principal muda
    var focadoId by remember { mutableStateOf<String?>(null) }
    val focado = jogos.find { it.chave == focadoId }
    // a volta dos Detalhes traz o id de uma fonte: o foco vai pro jogo que a tem
    val chaveInicial = jogos.find { j -> j.fontes.any { it.port.id == focoInicial } }?.chave
    val entrada = LocalEntrada.current.atual
    val pedirCapa = LocalPedirCapa.current
    // o console guardado pode ter saído do catálogo: aí vale Todos
    val consoleAtivo = console?.takeIf { escolhido -> jogos.any { Consoles.nomeDe(it.principal.port.plataforma) == escolhido } }
    val porConsole = jogos.filter { consoleAtivo == null || Consoles.nomeDe(it.principal.port.plataforma) == consoleAtivo }
    val lista = porConsole.filter { filtro.aceita(it.principal) }

    var painelAberto by remember { mutableStateOf(false) }
    var voltarAoChip by remember { mutableStateOf(false) }
    val chipConsole = remember { FocusRequester() }
    fun fecharPainel() {
        painelAberto = false
        voltarAoChip = true
    }
    // o foco volta ao chip só depois que o painel sai da tela (senão o foco do painel some junto)
    LaunchedEffect(voltarAoChip) {
        if (voltarAoChip) {
            chipConsole.focarQuandoPuder()
            voltarAoChip = false
        }
    }
    // com o painel aberto, B só fecha o painel (esta regra vence a de voltar de seção)
    BackHandler(enabled = painelAberto) { fecharPainel() }

    Fundo {
        Topo(Secao.PORTS, aoSecao)

        // filtros de estado e, à direita, o de console; as contas seguem o console
        Row(Modifier.padding(start = 21.dp, end = 26.dp, top = 46.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Filtro.entries.forEach { f ->
                var temFoco by remember { mutableStateOf(false) }
                val sel = f == filtro
                Row(
                    Modifier.onFocusChanged { temFoco = it.isFocused; if (it.isFocused) { focadoId = null; aoFocar(null) } }
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
                    Texto("${porConsole.count { f.aceita(it.principal) }}", Tipo.dica.copy(fontSize = 12.sp, color = if (sel) Color(0xFF4A4B52) else Cor.texto2))
                }
            }
            Box(Modifier.weight(1f))
            var temFocoConsole by remember { mutableStateOf(false) }
            val selConsole = consoleAtivo != null
            Row(
                Modifier.focusRequester(chipConsole)
                    .onFocusChanged { temFocoConsole = it.isFocused; if (it.isFocused) { focadoId = null; aoFocar(null) } }
                    .contornoFoco(temFocoConsole, raio = Raio, entrada = entrada)
                    .height(32.dp)
                    .clip(RoundedCornerShape(Raio))
                    .then(if (selConsole) Modifier.background(Cor.claro) else Modifier.border(1.dp, Cor.borda, RoundedCornerShape(Raio)))
                    .acionavel { painelAberto = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Texto("Console: ${consoleAtivo ?: "Todos"}", Tipo.secao.copy(fontSize = 13.sp, color = if (selConsole) Color(0xFF121212) else Cor.texto))
            }
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
            items(lista, key = { it.chave }) { jogo ->
                val pt = jogo.principal
                LaunchedEffect(pt.port.id) { pedirCapa(pt.port.id) }
                var temFoco by remember { mutableStateOf(false) }
                val sobe by animateDpAsState(if (temFoco) (-3).dp else 0.dp, tween(120), label = "sobe")
                Box(
                    Modifier
                        .then(if (jogo.chave == (chaveInicial ?: lista.firstOrNull()?.chave)) Modifier.focusRequester(primeiro) else Modifier)
                        .onFocusChanged {
                            temFoco = it.isFocused
                            if (it.isFocused) { focadoId = jogo.chave; aoFocar(pt) }
                        }
                        .acionavel { aoAcao(pt) }
                        // a capa sobe só no desenho: se subisse no layout, as vizinhas pareceriam "embaixo"
                        // dela e o ↓ andaria pro lado em vez de trazer a próxima fileira
                        .graphicsLayer { translationY = sobe.toPx() }
                        .contornoFoco(temFoco, entrada = entrada)
                        .aspectRatio(3f / 4f)
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp)),
                ) {
                    Arte(pt.port, Modifier.fillMaxSize(), url = pt.port.capaUrl, larguraMax = 360)
                    Selo(pt, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                    // mais de uma fonte: pílula embaixo à esquerda, espelho do selo
                    if (jogo.fontes.size > 1) Box(
                        Modifier.align(Alignment.BottomStart).padding(6.dp).height(20.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xC7000000)).padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Texto("${jogo.fontes.size} fontes", Tipo.botao.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cor.claro))
                    }
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
            val pt = focado?.principal
            val fontes = focado?.fontes?.size ?: 0
            if (pt != null) {
                Texto(Titulo.semParenteses(pt.port.titulo), Tipo.botao.copy(color = Cor.claro))
                if (fontes > 1) Texto("$fontes fontes", Tipo.dica.copy(fontSize = 12.5.sp))
                if (pt.estado != Estado.PRONTO) LinhaEstado(pt, longo = false)
            }
            Box(Modifier.weight(1f))
            Dicas(if (pt == null) emptyList() else if (Textos.naoInstalado(pt.estado)) listOf(Botao.A to "Detalhes") else listOf(Botao.A to Textos.de(pt.estado).acao, Botao.Y to "Detalhes"))
        }

        // painel de consoles, por cima de tudo; "Todos" é nulo
        if (painelAberto) PainelConsoles(
            itens = listOf<Pair<String?, Int>>(null to jogos.size) + Consoles.agrupar(jogos.map { it.principal }),
            atual = consoleAtivo,
            aoEscolher = { escolhido -> aoConsole(escolhido); fecharPainel() },
            aoFechar = { fecharPainel() },
        )
    }
}

/**
 * Painel lateral (direita, altura toda) com os consoles da lista. O foco começa
 * na escolha atual e não sai do painel até fechar (`exit` cancelado). Toque fora fecha.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BoxScope.PainelConsoles(
    itens: List<Pair<String?, Int>>,
    atual: String?,
    aoEscolher: (String?) -> Unit,
    aoFechar: () -> Unit,
) {
    val naAtual = remember { FocusRequester() }
    LaunchedEffect(Unit) { naAtual.focarQuandoPuder() }

    Box(Modifier.fillMaxSize().background(Color(0x73000000)).clickableSemFoco(aoFechar))
    Column(
        Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(300.dp)
            .background(Cor.elevado)
            .drawBehind { drawLine(Cor.borda, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx()) }
            .focusProperties { exit = { FocusRequester.Cancel } }
            .padding(start = 26.dp, end = 22.dp, top = 40.dp, bottom = 16.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itens.forEach { (nome, n) ->
                val sel = nome == atual
                ItemConsole(
                    nome = nome ?: "Todos",
                    n = n,
                    sel = sel,
                    foco = if (sel) naAtual else null,
                    aoAcionar = { aoEscolher(nome) },
                )
            }
        }
        Dicas(listOf(Botao.A to "Escolher", Botao.B to "Fechar"), Modifier.padding(top = 10.dp))
    }
}

/** Uma linha do painel: nome do console e a conta. Escolhido, fica claro como os filtros. */
@Composable
private fun ItemConsole(nome: String, n: Int, sel: Boolean, foco: FocusRequester?, aoAcionar: () -> Unit) {
    var temFoco by remember { mutableStateOf(false) }
    val entrada = LocalEntrada.current.atual
    Row(
        Modifier
            .then(if (foco != null) Modifier.focusRequester(foco) else Modifier)
            .onFocusChanged { temFoco = it.isFocused }
            .contornoFoco(temFoco, raio = Raio, entrada = entrada)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Raio))
            .then(if (sel) Modifier.background(Cor.claro) else Modifier.border(1.dp, Cor.borda, RoundedCornerShape(Raio)))
            .acionavel(aoAcionar)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Texto(nome, Tipo.botao.copy(fontSize = 14.sp, color = if (sel) Color(0xFF121212) else Cor.texto), Modifier.weight(1f))
        Texto("$n", Tipo.dica.copy(fontSize = 12.5.sp, color = if (sel) Color(0xFF4A4B52) else Cor.texto2), Modifier.padding(start = 10.dp))
    }
}
