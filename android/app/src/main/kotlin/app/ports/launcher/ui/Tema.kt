package app.ports.launcher.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ports.core.Tom

/** O visual do Hydra (globals.scss do fork), os mesmos tokens do lab (comum/tema.css). */
object Cor {
    val fundo = Color(0xFF121212)
    val fundoEscuro = Color(0xFF0D0D0D)
    val elevado = Color(0xFF1A1A1A)
    val claro = Color(0xFFF0F1F7)
    val texto = Color(0xFFD0D1D7)
    val texto2 = Color(0xFFA4A5AD)
    val borda = Color(0x14FFFFFF)
    val bordaForte = Color(0x38FFFFFF)
    val teal = Color(0xFF16B195)
    val tealClaro = Color(0xFF7FD8C6)
    val ok = Color(0xFF5FE39A)
    val aviso = Color(0xFFFFC107)
    val ruim = Color(0xFFFF7A8A)

    fun de(t: Tom) = when (t) {
        Tom.OK -> ok
        Tom.DOWNLOAD -> tealClaro
        Tom.AVISO -> aviso
        Tom.RUIM -> ruim
        Tom.NEUTRO -> texto
    }
}

val Raio = 8.dp

/** Noto Sans ainda não está embutida: o esqueleto usa a fonte do sistema (pendente). */
object Tipo {
    private val f = FontFamily.Default
    val corpo = TextStyle(fontFamily = f, fontSize = 14.sp, color = Cor.texto)
    val dica = TextStyle(fontFamily = f, fontSize = 13.sp, color = Cor.texto2)
    val sobre = TextStyle(fontFamily = f, fontSize = 12.sp, letterSpacing = 0.8.sp, color = Color(0xFFE6E7EC))
    val logo = TextStyle(fontFamily = f, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.3.sp, lineHeight = 32.sp)
    val botao = TextStyle(fontFamily = f, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val secao = TextStyle(fontFamily = f, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val rotulo = TextStyle(fontFamily = f, fontSize = 11.5.sp, letterSpacing = 0.8.sp, color = Cor.texto2)
}

/** A entrada em uso decide os botões desenhados na tela (P-23 do Roseta). */
enum class Entrada { XBOX, PLAYSTATION, TOQUE }

class EstadoEntrada { var atual by mutableStateOf(Entrada.XBOX) }
val LocalEntrada = compositionLocalOf { EstadoEntrada() }
