package app.ports.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Estado
import app.ports.core.Port
import app.ports.core.PortNaTela
import app.ports.core.Textos

/* ------------------------------------------------------------ arte
   Sem imagem no esqueleto: gradiente com a cor do jogo e o nome desenhado,
   como no lab. Port sem arte mostra as iniciais (P-08). */
@Composable
fun Arte(p: Port, modifier: Modifier = Modifier, comNome: Boolean = true, tamanhoNome: TextUnit = 13.sp) {
    val c = p.cores
    Box(
        modifier.background(
            if (c != null) Brush.radialGradient(listOf(Color(c[0]), Color(c[1]), Color(c[2])), center = Offset(0.72f * 400, 0.32f * 300), radius = 520f)
            else Brush.linearGradient(listOf(Color(0xFF1B1B1B), Color(0xFF1B1B1B))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (comNome) BasicText(
            if (c != null) p.logo else iniciais(p.titulo),
            Modifier.padding(10.dp),
            style = TextStyle(
                fontSize = tamanhoNome, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = tamanhoNome * 1.05f,
                color = if (c != null) Color(0xE0FFFFFF) else Cor.texto2, shadow = Shadow(Color(0x80000000), blurRadius = 8f),
            ),
        )
    }
}

fun iniciais(t: String) = t.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/* ------------------------------------------------------------ estado: símbolo + palavra, nunca só cor (P-42) */
@Composable
fun LinhaEstado(pt: PortNaTela, longo: Boolean = true, tamanho: TextUnit = 13.sp) {
    val t = Textos.de(pt.estado)
    val base = if (longo) t.longo else t.curto
    val texto = when (pt.estado) {
        Estado.BAIXANDO -> "$base · ${pt.progresso ?: 0}%"
        Estado.ATUALIZAR -> "$base: ${pt.port.versaoMaisNova}"
        else -> base
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0x8C000000)), contentAlignment = Alignment.Center) {
            BasicText(t.simbolo, style = TextStyle(color = Cor.de(t.tom), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
        BasicText(texto, style = TextStyle(color = Cor.de(t.tom), fontSize = tamanho, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun Selo(pt: PortNaTela, modifier: Modifier = Modifier) {
    val t = Textos.de(pt.estado)
    Box(modifier.size(20.dp).clip(CircleShape).background(Color(0xC7000000)), contentAlignment = Alignment.Center) {
        BasicText(t.simbolo, style = TextStyle(color = Cor.de(t.tom), fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun Barra(progresso: Int, modifier: Modifier = Modifier) {
    Box(modifier.height(3.dp).background(Color(0x1FFFFFFF))) {
        Box(Modifier.fillMaxWidth(progresso / 100f).height(3.dp).background(Cor.teal))
    }
}

/* ------------------------------------------------------------ botões do controle (P-23) */
enum class Botao { A, B, X, Y, L1, R1, MENU }

fun rotulo(b: Botao, e: Entrada): String = when (e) {
    Entrada.PLAYSTATION -> mapOf(Botao.A to "✕", Botao.B to "○", Botao.X to "□", Botao.Y to "△", Botao.L1 to "L1", Botao.R1 to "R1", Botao.MENU to "≡")[b]!!
    else -> mapOf(Botao.A to "A", Botao.B to "B", Botao.X to "X", Botao.Y to "Y", Botao.L1 to "LB", Botao.R1 to "RB", Botao.MENU to "☰")[b]!!
}

@Composable
fun Glifo(b: Botao, claro: Boolean = true) {
    val e = LocalEntrada.current.atual
    if (e == Entrada.TOQUE) return
    val ombro = b == Botao.L1 || b == Botao.R1
    Box(
        Modifier.height(if (ombro) 18.dp else 20.dp).widthIn(min = 20.dp)
            .clip(if (ombro) RoundedCornerShape(5.dp) else CircleShape)
            .background(if (ombro) Color(0xFF3A3A3A) else if (claro) Cor.claro else Color(0xFF121212))
            .padding(horizontal = if (ombro) 6.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(rotulo(b, e), style = TextStyle(fontSize = if (ombro) 10.5.sp else 11.sp, fontWeight = FontWeight.Bold, color = if (ombro || !claro) Cor.claro else Color(0xFF121212)))
    }
}

/** Botão do Hydra (button.scss): primário claro ou contorno. Não recebe foco:
 *  com controle, A e Y agem sobre o jogo em foco; o botão mostra o que eles fazem. */
@Composable
fun BotaoHydra(texto: String, primario: Boolean, botao: Botao?, modifier: Modifier = Modifier, aoClicar: () -> Unit = {}) {
    Row(
        modifier.clickableSemFoco(aoClicar).height(40.dp).clip(RoundedCornerShape(Raio))
            .then(if (primario) Modifier.background(Cor.claro) else Modifier.background(Color(0x59000000)).border(1.dp, Cor.bordaForte, RoundedCornerShape(Raio)))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (botao != null) Glifo(botao, claro = !primario)
        BasicText(texto, style = Tipo.botao.copy(color = if (primario) Color(0xFF121212) else Cor.claro))
    }
}

/* ------------------------------------------------------------ topo: seções com L1/R1 */
enum class Secao(val nome: String) { INICIO("Início"), PORTS("Ports"), AJUSTES("Ajustes") }

@Composable
fun Topo(atual: Secao, aoIr: (Secao) -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Glifo(Botao.L1)
        Secao.entries.forEach { s ->
            val sel = s == atual
            Column(Modifier.width(IntrinsicSize.Max).clickableSemFoco { aoIr(s) }.padding(top = 6.dp)) {
                BasicText(s.nome, style = Tipo.secao.copy(color = if (sel) Cor.claro else Cor.texto2))
                Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(2.dp).background(if (sel) Cor.claro else Color.Transparent))
            }
        }
        Glifo(Botao.R1)
        Box(Modifier.weight(1f))
        BasicText("9:41", style = Tipo.dica.copy(fontSize = 12.sp))
    }
}

/* ------------------------------------------------------------ dicas de botões, embaixo à direita */
@Composable
fun Dicas(itens: List<Pair<Botao, String>>, modifier: Modifier = Modifier) {
    if (LocalEntrada.current.atual == Entrada.TOQUE) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        itens.forEach { (b, t) ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Glifo(b, claro = false)
                BasicText(t, style = Tipo.dica.copy(fontSize = 12.5.sp))
            }
        }
    }
}

/** Contorno de foco: claro, grosso, sempre visível com controle (P-15). */
fun Modifier.contornoFoco(focado: Boolean, raio: Dp = 6.dp, entrada: Entrada): Modifier =
    if (focado && entrada != Entrada.TOQUE) this.border(3.dp, Cor.claro, RoundedCornerShape(raio + 3.dp)).padding(5.dp) else this.padding(5.dp)

@Composable
fun Texto(t: String, estilo: TextStyle, modifier: Modifier = Modifier, linhas: Int = Int.MAX_VALUE) =
    BasicText(t, modifier, style = estilo, maxLines = linhas, overflow = TextOverflow.Ellipsis)

@Composable
fun Fundo(conteudo: @Composable BoxScope.() -> Unit) = Box(Modifier.fillMaxSize().background(Cor.fundo), content = conteudo)

/** Clicável pelo toque, mas fora da navegação por controle. */
fun Modifier.clickableSemFoco(aoClicar: () -> Unit): Modifier = composed {
    this.focusProperties { canFocus = false }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = aoClicar)
}
