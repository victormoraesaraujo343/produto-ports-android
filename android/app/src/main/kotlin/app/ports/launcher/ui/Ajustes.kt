package app.ports.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * AJUSTES — deitado. Duas linhas: a chave das capas (SteamGridDB) e a lista de
 * ports. Sem texto além do nome e do valor (D-007). A chave nunca aparece na tela.
 */
@Composable
fun Ajustes(
    atualizadoEm: String?,
    aoSecao: (Secao) -> Unit,
    aoChaveMudou: () -> Unit,
    aoAtualizarLista: () -> Unit,
) {
    val repositorio = LocalRepositorio.current
    val entrada = LocalEntrada.current.atual
    val teclado = LocalSoftwareKeyboardController.current
    var comChave by remember { mutableStateOf(repositorio.chaveSteamGridDb.isNotBlank()) }
    var editando by remember { mutableStateOf(false) }
    var rascunho by remember { mutableStateOf("") }
    val primeiro = remember { FocusRequester() }
    val campo = remember { FocusRequester() }

    // B fecha o campo antes de sair da tela
    BackHandler(enabled = editando) { editando = false }

    fun gravar(chave: String) {
        repositorio.chaveSteamGridDb = chave
        comChave = chave.isNotBlank()
        rascunho = ""
        editando = false
        aoChaveMudou()
    }

    // abrir o campo põe o cursor nele e sobe o teclado; fechar devolve o foco à linha
    LaunchedEffect(editando) {
        if (editando) {
            campo.focarQuandoPuder()
            teclado?.show()
        } else {
            primeiro.focarQuandoPuder()
        }
    }

    Fundo {
        Topo(Secao.AJUSTES, aoSecao)

        Column(Modifier.padding(start = 32.dp, end = 32.dp, top = 54.dp).width(560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Linha(
                titulo = "Capas (SteamGridDB)",
                valor = if (comChave) "Chave salva" else "Sem chave",
                entrada = entrada,
                modifier = Modifier.focusRequester(primeiro),
                aoAcionar = { editando = true },
            )
            if (editando) Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BasicTextField(
                    value = rascunho,
                    onValueChange = { rascunho = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { gravar(rascunho.trim()) }),
                    textStyle = Tipo.corpo.copy(fontSize = 14.sp, color = Cor.claro),
                    cursorBrush = SolidColor(Cor.claro),
                    decorationBox = { interno ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(Raio))
                                .background(Color(0x59000000))
                                .border(1.dp, Cor.bordaForte, RoundedCornerShape(Raio))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            if (rascunho.isEmpty()) Texto("Cole a chave", Tipo.dica)
                            interno()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(campo),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoFocavel("Salvar", primario = true, botao = Botao.A, aoAcionar = { gravar(rascunho.trim()) })
                    if (comChave) BotaoFocavel("Remover", primario = false, botao = null, aoAcionar = { gravar("") })
                }
            }
            Linha(
                titulo = "Lista de ports",
                valor = atualizadoEm?.let { "Atualizada ${quando(it)}" } ?: "—",
                entrada = entrada,
                aoAcionar = aoAtualizarLista,
            )
        }
    }
}

/** Uma linha focável: nome à esquerda, valor à direita. */
@Composable
private fun Linha(titulo: String, valor: String, entrada: Entrada, modifier: Modifier = Modifier, aoAcionar: () -> Unit) {
    var temFoco by remember { mutableStateOf(false) }
    Row(
        modifier.onFocusChanged { temFoco = it.isFocused }
            .contornoFoco(temFoco, raio = Raio, entrada = entrada)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Raio))
            .background(Color(0x59000000))
            .acionavel(aoAcionar)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Texto(titulo, Tipo.botao.copy(color = Cor.claro), Modifier.weight(1f))
        Texto(valor, Tipo.corpo.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Cor.texto))
    }
}

/** "agora", "há 3 h", "ontem", "há 4 dias" ou a data. Texto que não é data sai como veio. */
internal fun quando(iso: String, agora: Instant = Instant.now()): String {
    val instante = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
    val minutos = Duration.between(instante, agora).toMinutes()
    val dias = minutos / (60 * 24)
    return when {
        minutos < 2 -> "agora"
        minutos < 60 -> "há $minutos min"
        minutos < 60 * 24 -> "há ${minutos / 60} h"
        dias == 1L -> "ontem"
        dias < 7 -> "há $dias dias"
        else -> DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault()).format(instante)
    }
}
