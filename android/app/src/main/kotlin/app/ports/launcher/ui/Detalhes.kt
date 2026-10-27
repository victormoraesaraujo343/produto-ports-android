package app.ports.launcher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Estado
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
fun Detalhes(pt: PortNaTela, aoAcao: (PortNaTela) -> Unit, aoAtualizar: (PortNaTela) -> Unit, aoVoltar: () -> Unit) {
    val p = pt.port
    val repositorio = LocalRepositorio.current
    val acao = if (pt.estado == Estado.NAO_RODA) "Instalar mesmo assim" else Textos.de(pt.estado).acao
    val entrada = LocalEntrada.current.atual
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    // a release (versão, tamanho) e as capas chegam aqui, depois que a tela abre
    LaunchedEffect(p.id) {
        repositorio.detalhar(p.id)
        repositorio.capas(p.id)
    }
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
                Texto(listOfNotNull(p.nomePort, plataforma).joinToString(" · "), Tipo.corpo.copy(fontSize = 13.sp, color = Color(0xFFE6E7EC)), Modifier.padding(top = 4.dp))
                Box(Modifier.padding(top = 8.dp)) { LinhaEstado(pt) }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoFocavel(acao, primario = pt.estado != Estado.NAO_RODA, botao = Botao.A, focoRef = principal, aoAcionar = { aoAcao(pt) })
                    if (pt.estado == Estado.ATUALIZAR) BotaoFocavel("Atualizar", primario = false, botao = null, aoAcionar = { aoAtualizar(pt) })
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
                Bloco("FICHA", entrada) {
                    if (p.tipo.isNotBlank()) Ficha("Tipo", p.tipo.replaceFirstChar { it.uppercase() })
                    Ficha("Versão", pt.versaoInstalada ?: p.versaoMaisNova ?: "—")
                    if (p.tamanho.isNotBlank()) Ficha("Tamanho", p.tamanho)
                    Ficha("Projeto", p.repo)
                }
            }
        }
        Dicas(listOf(Botao.B to "Voltar"),Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 10.dp))
        if (visorAberto && imagens.isNotEmpty()) {
            Visualizador(imagens, atual, aoMudar = { irPara(it) }, aoFechar = { fecharVisor() })
        }
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

/** Nome da plataforma pra mostrar, ou nulo quando o catálogo não sabe (`other`). */
private fun plataformaMostrada(plataforma: String): String? =
    plataforma.takeIf { it.isNotBlank() && !it.equals("other", ignoreCase = true) && !it.equals("unknown", ignoreCase = true) }

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
