package app.ports.launcher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Datas
import app.ports.core.Estado
import app.ports.core.JogoNaTela
import app.ports.core.Plataformas
import app.ports.core.PortNaTela
import app.ports.core.Textos
import app.ports.core.Titulo
import app.ports.core.Tom
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * DETALHES — deitado. O estado que a capa resume vira a lista do que o port
 * precisa, sempre visível (aprovado na R1). A ação principal fica no mesmo
 * lugar e muda as palavras (P-26). Só aqui aparecem versão, tamanho e projeto (D-007).
 */
@Composable
fun Detalhes(jogo: JogoNaTela, aoAcao: (PortNaTela) -> Unit, aoAtualizar: (PortNaTela) -> Unit, aoInstalar: (PortNaTela) -> Unit, aoVoltar: () -> Unit) {
    // a fonte principal é o que a tela mostra (capa, fundo, ficha); as outras ficam na janela de fontes
    val pt = jogo.principal
    val p = pt.port
    val fontes = jogo.fontes
    val naoInstalado = Textos.naoInstalado(pt.estado)
    val instalada = pt.versaoInstalada != null
    // sem instalar e com mais de uma fonte, o botão principal abre a janela em vez de instalar direto
    val escolhe = naoInstalado && fontes.size > 1
    val acao = if (escolhe) "Instalar" else if (pt.estado == Estado.NAO_RODA) "Instalar mesmo assim" else Textos.de(pt.estado).acao
    val entrada = LocalEntrada.current.atual
    val repositorio = LocalRepositorio.current
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    // a release (versão, tamanho) e as capas chegam aqui, depois que a tela abre
    LaunchedEffect(p.id) {
        repositorio.detalhar(p.id)
        repositorio.capas(p.id)
    }
    // com mais de uma fonte, a versão e o tamanho de cada uma chegam pra janela
    LaunchedEffect(jogo.chave) { if (fontes.size > 1) repositorio.detalharFontes(fontes.map { it.port.id }) }
    // a galeria também: até chegar (null), a tela não mostra nada dela
    var galeria by remember(p.id) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(p.id) { galeria = repositorio.galeria(p.id) }
    val imagens = galeria.orEmpty()
    var foco by remember(p.id) { mutableIntStateOf(0) }      // miniatura e visor
    var fundo by remember(p.id) { mutableIntStateOf(0) }     // imagem do fundo agora
    var saltos by remember(p.id) { mutableIntStateOf(0) }    // cada salto manual reinicia a contagem de 6 s
    var visorAberto by remember(p.id) { mutableStateOf(false) }
    val atual = if (imagens.isEmpty()) 0 else foco.coerceIn(0, imagens.lastIndex)
    val focos = remember(imagens.size) { List(imagens.size) { FocusRequester() } }
    val reduzido = remember { semAnimacao(contexto) }

    // o fundo passa sozinho a cada 6 s; um salto manual (miniatura) recomeça a contagem
    LaunchedEffect(imagens.size, saltos) {
        if (imagens.size > 1) {
            while (true) {
                delay(6000)
                fundo = (fundo + 1) % imagens.size
            }
        }
    }
    fun irPara(i: Int) {
        foco = i
        fundo = i
        saltos++
    }
    fun fecharVisor() {
        visorAberto = false
        // a miniatura volta a ter o foco, como antes de abrir
        escopo.launch { focos.getOrNull(atual)?.focarQuandoPuder() }
    }

    val principal = remember { FocusRequester() }
    // janela de fontes: o botão que a abriu recebe o foco de volta quando ela fecha
    val trocarFoco = remember { FocusRequester() }
    var fontesAbertas by remember(jogo.chave) { mutableStateOf(false) }
    var origemFontes by remember { mutableStateOf<FocusRequester?>(null) }
    var voltarFoco by remember { mutableStateOf<FocusRequester?>(null) }
    fun abrirFontes(origem: FocusRequester) { origemFontes = origem; fontesAbertas = true }
    fun fecharFontes() { fontesAbertas = false; voltarFoco = origemFontes }
    // o foco só volta depois que a janela sai da tela (senão o foco dela some junto)
    LaunchedEffect(voltarFoco) { voltarFoco?.let { it.focarQuandoPuder(); voltarFoco = null } }
    Fundo {
        // sem galeria: hero, capa borrada ou placeholder, como antes. Com galeria, o slideshow cobre tudo.
        Arte(p, Modifier.fillMaxSize().padding(start = 270.dp), comNome = false, url = p.heroUrl ?: p.capaUrl, larguraMax = 960, desfocar = p.heroUrl == null)
        AnimatedVisibility(visible = imagens.isNotEmpty(), modifier = Modifier.fillMaxSize(), enter = fadeIn(tween(900)), exit = fadeOut(tween(0))) {
            Slideshow(imagens, fundo, reduzido)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.34f to Cor.fundo, 0.52f to Color(0xD1121212), 0.75f to Color(0x73121212), 1f to Color(0xB3121212))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0xEB121212))))

        // ‹ só no toque; com controle, B volta (dica embaixo)
        if (entrada == Entrada.TOQUE) Box(Modifier.padding(start = 20.dp, top = 8.dp).clickableSemFoco(aoVoltar)) {
            Texto("‹", Tipo.logo.copy(fontSize = 28.sp))
        }

        // o Odin tem ~830×467 dp (1920×1080 a 369 dpi), não 960×540: a coluna da direita pega o que sobrar
        Row(Modifier.fillMaxSize().padding(top = 28.dp, end = 32.dp)) {
            // coluna da ação: galeria em cima do nome, depois título, estado e botões
            Column(Modifier.padding(start = 32.dp).width(400.dp)) {
                if (imagens.isNotEmpty()) {
                    Visor(imagens[atual]) { visorAberto = true }
                    // com muitas imagens a faixa rola na horizontal; a folga de 6 dp deixa o contorno de foco aparecer
                    Row(Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        imagens.forEachIndexed { i, url ->
                            Miniatura(url, focos[i], aoFocar = { irPara(i) }, aoAbrir = { irPara(i); visorAberto = true })
                        }
                    }
                }
                Texto(Titulo.semParenteses(p.titulo), Tipo.logo.copy(fontSize = 28.sp), Modifier.padding(top = 6.dp))
                val plataforma = plataformaMostrada(p.plataforma)
                // sem instalar e com várias fontes, a linha diz quantas são (a janela mostra quais)
                val linha = if (naoInstalado && fontes.size > 1) listOfNotNull(plataforma, "${fontes.size} fontes") else listOfNotNull(p.nomePort, plataforma)
                Texto(linha.joinToString(" · "), Tipo.corpo.copy(fontSize = 13.sp, color = Color(0xFFE6E7EC)), Modifier.padding(top = 4.dp))
                Box(Modifier.padding(top = 8.dp)) { LinhaEstado(pt) }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoFocavel(acao, primario = pt.estado != Estado.NAO_RODA, botao = Botao.A, focoRef = principal, aoAcionar = { if (escolhe) abrirFontes(principal) else aoAcao(pt) })
                    if (pt.estado == Estado.ATUALIZAR) BotaoFocavel("Atualizar", primario = false, botao = null, aoAcionar = { aoAtualizar(pt) })
                    if (instalada && fontes.size > 1) BotaoFocavel("Trocar fonte", primario = false, botao = null, focoRef = trocarFoco, aoAcionar = { abrirFontes(trocarFoco) })
                    BotaoFocavel("GitHub ↗", primario = false, botao = null, aoAcionar = { abrirGithub(contexto, p.repo) })
                }
                LaunchedEffect(Unit) { principal.focarQuandoPuder() }
            }
            // coluna da direita: o que o port precisa, Sobre e Ficha. Rola com ↓.
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(start = 24.dp).verticalScroll(rememberScrollState()).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Texto("O QUE O PORT PRECISA", Tipo.rotulo, Modifier.padding(top = 4.dp))
                Precisa(pt)
                if (p.descricao.isNotBlank()) Bloco("SOBRE", entrada) { Texto(p.descricao, Tipo.corpo.copy(fontSize = 13.5.sp, lineHeight = 20.sp)) }
                Bloco(if (instalada && fontes.size > 1) "FICHA · FONTE INSTALADA" else "FICHA", entrada) {
                    if (p.tipo.isNotBlank()) Ficha("Tipo", p.tipo.replaceFirstChar { it.uppercase() })
                    Ficha("Versão", pt.versaoInstalada ?: p.versaoMaisNova ?: "—")
                    if (p.tamanho.isNotBlank()) Ficha("Tamanho", p.tamanho)
                    Ficha("Projeto", p.repo)
                    if (instalada && fontes.size > 1) Ficha("Outras fontes", fontes.filter { it.port.id != p.id }.joinToString(", ") { it.port.nomePort })
                    if (p.aiFlag) Ficha("Feito com IA", "Sim")
                }
            }
        }
        Dicas(listOf(Botao.B to "Voltar"),Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 10.dp))
        if (visorAberto && imagens.isNotEmpty()) {
            Visualizador(imagens, atual, aoMudar = { irPara(it) }, aoFechar = { fecharVisor() })
        }
        if (fontesAbertas) JanelaFontes(jogo, aoEscolher = aoInstalar, aoFechar = { fecharFontes() })
    }
}

/** Abre a página do projeto no navegador. Sem navegador, avisa em um toque. */
private fun abrirGithub(contexto: Context, repo: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$repo")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        contexto.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(contexto, "Sem navegador.", Toast.LENGTH_SHORT).show()
    }
}

/** Nome da plataforma pra mostrar, ou nulo quando o catálogo não sabe (Outros). */
private fun plataformaMostrada(plataforma: String): String? =
    plataforma.takeIf { it.isNotBlank() && it != Plataformas.OUTROS }

/** Só fatos que o app sabe de verdade: instalado, versão nova, roda aqui. */
@Composable
private fun Precisa(pt: PortNaTela) {
    val p = pt.port
    val versao = pt.versaoInstalada
    val itens = buildList {
        add(if (versao != null) Triple(Tom.OK, "Port instalado", "") else Triple(Tom.NEUTRO, "Port não instalado", ""))
        val nova = p.versaoMaisNova
        if (versao != null && nova != null) {
            add(if (nova == versao) Triple(Tom.OK, "Na versão mais nova", "") else Triple(Tom.DOWNLOAD, "Versão nova: $nova", ""))
        }
        if (pt.estado == Estado.NAO_RODA) add(Triple(Tom.RUIM, "Não roda neste aparelho", p.motivoNaoRoda ?: ""))
        else add(Triple(Tom.OK, "Roda neste aparelho", ""))
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itens.forEach { (tom, titulo, detalhe) ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0x0FFFFFFF)), contentAlignment = Alignment.Center) {
                    Texto(when (tom) { Tom.OK -> "✓"; Tom.AVISO -> "!"; Tom.RUIM -> "×"; Tom.DOWNLOAD -> "↓"; Tom.NEUTRO -> "+" }, Tipo.botao.copy(fontSize = 11.sp, color = Cor.de(tom)))
                }
                Column {
                    Texto(titulo, Tipo.botao.copy(fontSize = 13.5.sp, color = Cor.claro))
                    if ((tom == Tom.AVISO || tom == Tom.RUIM) && detalhe.isNotEmpty()) Texto(detalhe, Tipo.dica.copy(fontSize = 12.5.sp))
                }
            }
        }
    }
}

@Composable
private fun Bloco(titulo: String, entrada: Entrada, conteudo: @Composable () -> Unit) {
    var temFoco by remember { mutableStateOf(false) }
    Column(
        Modifier.onFocusChanged { temFoco = it.isFocused }
            .contornoFoco(temFoco, raio = Raio, entrada = entrada)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Raio))
            .background(Color(0xB80D0D0D))
            .border(1.dp, Cor.borda, RoundedCornerShape(Raio))
            .acionavel { }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Texto(titulo, Tipo.rotulo, Modifier.padding(bottom = 4.dp))
        conteudo()
    }
}

@Composable
private fun Ficha(rotulo: String, valor: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Texto(rotulo, Tipo.dica.copy(fontSize = 13.sp), Modifier.width(100.dp))
        Texto(valor, Tipo.corpo.copy(fontSize = 13.sp))
    }
}

/**
 * Janela de fontes (como o "Baixar" do Hydra), centrada: uma linha por port do jogo, a sugerida primeiro.
 * O foco fica preso aqui até fechar. B fecha; toque no fundo escuro também.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BoxScope.JanelaFontes(jogo: JogoNaTela, aoEscolher: (PortNaTela) -> Unit, aoFechar: () -> Unit) {
    val primeira = remember { FocusRequester() }
    BackHandler { aoFechar() }
    LaunchedEffect(Unit) { primeira.focarQuandoPuder() }

    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickableSemFoco(aoFechar))
    Column(
        Modifier
            .align(Alignment.Center)
            .width(560.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Cor.elevado)
            .border(1.dp, Cor.borda, RoundedCornerShape(12.dp))
            .focusProperties { exit = { FocusRequester.Cancel } }
            .clickableSemFoco { }
            .padding(vertical = 20.dp, horizontal = 16.dp),
    ) {
        Texto("Instalar ${Titulo.semParenteses(jogo.principal.port.titulo)}", Tipo.botao.copy(fontSize = 17.sp, color = Cor.claro))
        Texto("Escolha de onde baixar. A sugerida vem marcada.", Tipo.dica.copy(fontSize = 12.5.sp), Modifier.padding(top = 4.dp))
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            jogo.fontes.forEachIndexed { i, pt ->
                LinhaFonte(
                    pt,
                    sugerida = pt.port.id == jogo.sugerida.port.id,
                    foco = if (i == 0) primeira else null,
                    // a instalada só fecha a janela; as outras viram a instalação
                    aoAcionar = { if (pt.versaoInstalada == null) aoEscolher(pt); aoFechar() },
                )
            }
        }
        Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(1.dp).background(Cor.borda))
        Dicas(listOf(Botao.A to "Instalar", Botao.B to "Fechar"), Modifier.padding(top = 10.dp))
    }
}

/** Uma fonte na janela: nome e etiquetas; embaixo, dono, versão, data e estrelas. Tamanho à direita. */
@Composable
private fun LinhaFonte(pt: PortNaTela, sugerida: Boolean, foco: FocusRequester?, aoAcionar: () -> Unit) {
    val p = pt.port
    var temFoco by remember { mutableStateOf(false) }
    Row(
        Modifier
            .then(if (foco != null) Modifier.focusRequester(foco) else Modifier)
            .onFocusChanged { temFoco = it.isFocused }
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (temFoco) Color(0x12FFFFFF) else Color.Transparent)
            .then(if (temFoco) Modifier.border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(8.dp)) else Modifier)
            .drawBehind { if (temFoco) drawRect(Cor.claro, size = Size(3.dp.toPx(), size.height)) }
            .acionavel(aoAcionar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Texto(p.nomePort, Tipo.botao.copy(fontSize = 14.sp, color = Cor.claro))
                if (sugerida) Etiqueta("Sugerida", Color(0x2E16B195), Cor.tealClaro)
                if (pt.versaoInstalada != null) Etiqueta("Instalada", Color(0x265FE39A), Cor.ok)
                if (p.aiFlag) Etiqueta("Feito com IA", Color(0x14FFFFFF), Cor.texto)
            }
            Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val dica = Tipo.dica.copy(fontSize = 12.5.sp)
                Texto(p.repo.substringBefore('/'), dica)
                Texto(p.versaoMaisNova ?: "—", dica)
                Datas.curta(p.atualizadoEm)?.let { Texto("atualizado $it", dica) }
                p.estrelas?.let { Texto("★ $it", dica) }
            }
        }
        Texto(p.tamanho, Tipo.botao.copy(fontSize = 13.sp, color = Cor.claro), Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun Etiqueta(texto: String, fundo: Color, cor: Color) {
    Texto(texto, Tipo.dica.copy(fontSize = 11.sp, color = cor), Modifier.clip(RoundedCornerShape(4.dp)).background(fundo).padding(horizontal = 6.dp, vertical = 1.dp))
}
