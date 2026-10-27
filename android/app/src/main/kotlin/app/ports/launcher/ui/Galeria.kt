package app.ports.launcher.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Imagem da galeria: carrega pelo repositório e aparece quando chega (nada enquanto isso). */
@Composable
fun ImagemGaleria(url: String, larguraMax: Int, escala: ContentScale, modifier: Modifier = Modifier) {
    val repositorio = LocalRepositorio.current
    val imagem by produceState<ImageBitmap?>(null, url, larguraMax) { value = repositorio.imagens.carregar(url, larguraMax) }
    imagem?.let { img ->
        Image(
            img,
            contentDescription = null,
            modifier = modifier,
            contentScale = escala,
            // pixel art pequena fica nítida; imagem grande suaviza
            filterQuality = if (img.width < 400) FilterQuality.None else FilterQuality.Low,
        )
    }
}

/** Fundo do Detalhes: as imagens passam em crossfade de 1,2 s, cada uma com zoom lento de 7 s. Sem animação no sistema, só troca. */
@Composable
fun Slideshow(imagens: List<String>, indice: Int, semAnimacao: Boolean, modifier: Modifier = Modifier) {
    if (imagens.isEmpty()) return
    Crossfade(
        targetState = indice.coerceIn(0, imagens.lastIndex),
        modifier = modifier.fillMaxSize().background(Cor.fundo),
        animationSpec = tween(if (semAnimacao) 0 else 1200),
        label = "fundo",
    ) { i ->
        CamadaFundo(imagens[i], semZoom = semAnimacao)
    }
}

@Composable
private fun CamadaFundo(url: String, semZoom: Boolean) {
    val repositorio = LocalRepositorio.current
    val imagem by produceState<ImageBitmap?>(null, url) { value = repositorio.imagens.carregar(url, 960) }
    val zoom = remember { Animatable(0f) }
    val pronta = imagem != null
    // o zoom começa quando a imagem aparece, não antes
    LaunchedEffect(pronta) {
        if (pronta && !semZoom) zoom.animateTo(1f, tween(7000, easing = EaseInOut))
    }
    imagem?.let { img ->
        Image(
            img,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val t = zoom.value
                val escala = 1.04f + 0.10f * t
                scaleX = escala
                scaleY = escala
                translationX = -0.02f * size.width * t
                translationY = -0.015f * size.height * t
            },
        )
    }
}

/** Miniatura de 56×38, sem legenda. Com foco, contorno claro por fora. A (ou toque) abre o visor em tela cheia. */
@Composable
fun Miniatura(url: String, focoRef: FocusRequester, aoFocar: () -> Unit, aoAbrir: () -> Unit) {
    var temFoco by remember { mutableStateOf(false) }
    val entrada = LocalEntrada.current.atual
    Box(
        Modifier
            .size(56.dp, 38.dp)
            .focusRequester(focoRef)
            .onFocusChanged {
                temFoco = it.isFocused
                if (it.isFocused) aoFocar()
            }
            .marcaDeFoco(temFoco, 6.dp, entrada)
            .background(Color(0xFF1B1B1B))
            .clip(RoundedCornerShape(6.dp))
            .acionavel(aoAbrir),
    ) {
        ImagemGaleria(url, larguraMax = 160, escala = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/** Visor da galeria: 400×225 (16:9), a imagem inteira. Foco ou toque abrem em tela cheia com A. */
@Composable
fun Visor(url: String, aoAbrir: () -> Unit) {
    var temFoco by remember { mutableStateOf(false) }
    val entrada = LocalEntrada.current.atual
    Box(
        Modifier
            .size(400.dp, 225.dp)
            .onFocusChanged { temFoco = it.isFocused }
            .marcaDeFoco(temFoco, Raio, entrada)
            .clip(RoundedCornerShape(Raio))
            .background(Color(0xFF0B0B0B))
            .border(1.dp, Cor.borda, RoundedCornerShape(Raio))
            .acionavel(aoAbrir),
        contentAlignment = Alignment.Center,
    ) {
        ImagemGaleria(url, larguraMax = 960, escala = ContentScale.Fit, modifier = Modifier.fillMaxSize())
    }
}

/** Contorno claro por fora da caixa, sem empurrar os vizinhos. Com controle, só quando tem foco. */
private fun Modifier.marcaDeFoco(focado: Boolean, raio: Dp, entrada: Entrada): Modifier = drawBehind {
    if (focado && entrada != Entrada.TOQUE) {
        val folga = 4.5.dp.toPx()
        drawRoundRect(
            color = Cor.claro,
            topLeft = Offset(-folga, -folga),
            size = Size(size.width + 2 * folga, size.height + 2 * folga),
            cornerRadius = CornerRadius(raio.toPx() + folga),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

/**
 * Tela cheia da galeria: a imagem em até 86% × 82%. ←/→ trocam; B (ou toque) fecha.
 * Fecha no KeyUp: se fechasse no KeyDown, o KeyUp do B chegaria ao Detalhes e a
 * MainActivity voltaria uma tela.
 */
@Composable
fun Visualizador(imagens: List<String>, indice: Int, aoMudar: (Int) -> Unit, aoFechar: () -> Unit) {
    val foco = remember { FocusRequester() }
    val n = imagens.size
    LaunchedEffect(Unit) { foco.focarQuandoPuder() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF0000000))
            .focusRequester(foco)
            .focusable()
            .onKeyEvent { e ->
                when {
                    e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight -> { aoMudar((indice + 1) % n); true }
                    e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft -> { aoMudar((indice - 1 + n) % n); true }
                    e.type == KeyEventType.KeyUp && (e.key == Key.Back || e.key == Key.Escape) -> { aoFechar(); true }
                    else -> false
                }
            }
            .pointerInput(Unit) { detectTapGestures { aoFechar() } },
        contentAlignment = Alignment.Center,
    ) {
        ImagemGaleria(imagens[indice], larguraMax = 1280, escala = ContentScale.Fit, modifier = Modifier.fillMaxWidth(0.86f).fillMaxHeight(0.82f))
        Dicas(listOf(Botao.B to "Fechar"), Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 10.dp))
    }
}

/** Liga o “Remover animações” do sistema (escala de animação zero). */
fun semAnimacao(contexto: Context): Boolean =
    Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
