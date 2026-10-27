package app.ports.launcher.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Estado
import app.ports.core.PortNaTela
import app.ports.core.Textos
import app.ports.core.Tom

/**
 * DETALHES — deitado. O estado que a capa resume vira a lista do que o port
 * precisa, sempre visível (aprovado na R1). A ação principal fica no mesmo
 * lugar e muda as palavras (P-26).
 */
@Composable
fun Detalhes(pt: PortNaTela, aoAcao: (PortNaTela) -> Unit, aoVoltar: () -> Unit) {
    val p = pt.port
    val t = Textos.de(pt.estado)
    val acao = if (pt.estado == Estado.NAO_RODA) "Instalar mesmo assim" else t.acao
    val entrada = LocalEntrada.current.atual

    Fundo {
        Arte(p, Modifier.fillMaxSize().padding(start = 270.dp), comNome = false)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.34f to Cor.fundo, 0.52f to Color(0xD1121212), 0.75f to Color(0x73121212), 1f to Color(0xB3121212))))

        // ‹ só no toque; com controle, B volta (dica embaixo)
        if (entrada == Entrada.TOQUE) Box(Modifier.padding(start = 20.dp, top = 8.dp).clickableSemFoco(aoVoltar)) {
            Texto("‹", Tipo.logo.copy(fontSize = 28.sp))
        }

        Row(Modifier.fillMaxSize().padding(top = 50.dp)) {
            // coluna da ação
            Column(Modifier.padding(start = 32.dp).width(400.dp)) {
                Texto(p.logo, Tipo.logo.copy(fontSize = 28.sp))
                Texto("${p.nomePort} · ${p.plataforma}", Tipo.corpo.copy(fontSize = 13.sp, color = Color(0xFFE6E7EC)), Modifier.padding(top = 4.dp))
                Box(Modifier.padding(top = 8.dp)) { LinhaEstado(pt) }
                val principal = remember { FocusRequester() }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    var temFoco by remember { mutableStateOf(false) }
                    Box(
                        Modifier.focusRequester(principal)
                            .onFocusChanged { temFoco = it.isFocused }
                            .contornoFoco(temFoco, raio = Raio, entrada = entrada)
                            .clip(RoundedCornerShape(Raio))
                            .acionavel { aoAcao(pt) },
                    ) { BotaoHydra(acao, pt.estado != Estado.NAO_RODA, Botao.A, aoClicar = { aoAcao(pt) }) } // o toque cai no botão; A/Enter, no Box
                }
                LaunchedEffect(Unit) { principal.focarQuandoPuder() }

                Texto("O QUE O PORT PRECISA", Tipo.rotulo, Modifier.padding(top = 14.dp, bottom = 4.dp))
                Precisa(pt)
            }
            // coluna de informação: blocos focáveis pra rolar com o direcional
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 40.dp, end = 30.dp, bottom = 40.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Bloco("SOBRE", entrada) { Texto(p.descricao, Tipo.corpo.copy(fontSize = 13.5.sp, lineHeight = 20.sp)) }
                Bloco("FICHA", entrada) {
                    Ficha("Tipo", p.tipo.replaceFirstChar { it.uppercase() })
                    Ficha("Versão", pt.versaoInstalada ?: "não instalado")
                    Ficha("Tamanho", p.tamanho)
                    Ficha("Projeto", p.repo)
                }
            }
        }
        Dicas(listOf(Botao.B to "Voltar"),Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 10.dp))
    }
}

@Composable
private fun Precisa(pt: PortNaTela) {
    val p = pt.port
    val instalado = pt.estado !in setOf(Estado.DISPONIVEL, Estado.NAO_RODA)
    val itens = buildList {
        if (pt.estado == Estado.NAO_RODA) {
            add(Triple(Tom.RUIM, "Não roda neste aparelho", p.motivoNaoRoda ?: ""))
            add(Triple(Tom.NEUTRO, "Port não instalado", "${p.tamanho} pra baixar"))
        } else {
            add(if (instalado) Triple(Tom.OK, "Port instalado", "versão ${pt.versaoInstalada ?: "?"}") else Triple(Tom.NEUTRO, "Port não instalado", "${p.tamanho} pra baixar do GitHub"))
            when (pt.estado) {
                Estado.ATUALIZAR -> add(Triple(Tom.DOWNLOAD, "Versão nova: ${p.versaoMaisNova}", "Dá pra jogar agora e atualizar depois"))
                Estado.BAIXANDO -> add(Triple(Tom.DOWNLOAD, "Baixando a versão nova · ${pt.progresso ?: 0}%", "Dá pra jogar a versão atual enquanto isso"))
                else -> if (instalado) add(Triple(Tom.OK, "Na versão mais nova", ""))
            }
            if (pt.estado == Estado.FALTA_ARQUIVO || pt.estado == Estado.ARQUIVO_ERRADO || !instalado) add(Triple(Tom.AVISO, "Falta o arquivo do jogo", "Serve: ${p.arquivoQueServe}"))
            else add(Triple(Tom.OK, "Arquivo do jogo conferido", p.arquivoQueServe))
            add(Triple(Tom.OK, "Roda neste aparelho", "Pelo que o catálogo pede"))
        }
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
