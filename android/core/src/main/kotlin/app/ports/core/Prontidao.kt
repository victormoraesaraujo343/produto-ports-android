package app.ports.core

/** O estado de um port do ponto de vista do jogador: o que aparece na capa. */
enum class Estado { PRONTO, BAIXANDO, ATUALIZAR, FALTA_ARQUIVO, ARQUIVO_ERRADO, DISPONIVEL, NAO_RODA }

enum class Arquivo { FALTA, CONFERIDO, NAO_CONFERE }

/**
 * As cinco perguntas da Prontidão (ARCHITECTURE.md): o port está instalado? Na
 * versão mais nova? O arquivo do jogo foi escolhido? Ele confere? O aparelho
 * aguenta? `baixando` é o progresso (0–100) de um download em andamento.
 */
data class Fatos(
    val instalado: Boolean,
    val versaoInstalada: String?,
    val versaoMaisNova: String?,
    val arquivo: Arquivo,
    val rodaAqui: Boolean,
    val baixando: Int? = null,
)

object Prontidao {
    /**
     * Uma resposta só, na ordem em que o jogador precisa agir: o que impede de
     * jogar vem antes do que é só melhoria. Nunca arredonda pra cima (D-032 do
     * Roseta): sem arquivo conferido, não é "pronto".
     */
    fun calcular(f: Fatos): Estado = when {
        !f.rodaAqui -> Estado.NAO_RODA
        !f.instalado -> Estado.DISPONIVEL
        f.baixando != null -> Estado.BAIXANDO
        f.arquivo == Arquivo.FALTA -> Estado.FALTA_ARQUIVO
        f.arquivo == Arquivo.NAO_CONFERE -> Estado.ARQUIVO_ERRADO
        f.versaoMaisNova != null && f.versaoMaisNova != f.versaoInstalada -> Estado.ATUALIZAR
        else -> Estado.PRONTO
    }
}

/** Tom visual do estado; a cor nunca vem sozinha, sempre com símbolo e palavra (P-42). */
enum class Tom { OK, DOWNLOAD, AVISO, RUIM, NEUTRO }

data class Texto(val simbolo: String, val curto: String, val longo: String, val acao: String, val tom: Tom)

/** As palavras de cada estado — as mesmas do lab (laboratorio/comum/dados.js). */
object Textos {
    fun de(e: Estado): Texto = when (e) {
        Estado.PRONTO -> Texto("✓", "Pronto", "Pronto pra jogar", "Jogar", Tom.OK)
        Estado.BAIXANDO -> Texto("↓", "Baixando", "Baixando a atualização", "Jogar mesmo assim", Tom.DOWNLOAD)
        Estado.ATUALIZAR -> Texto("↑", "Atualização", "Tem versão nova", "Jogar", Tom.DOWNLOAD)
        Estado.FALTA_ARQUIVO -> Texto("!", "Falta o jogo", "Falta o arquivo do jogo", "Escolher arquivo", Tom.AVISO)
        Estado.ARQUIVO_ERRADO -> Texto("!", "Arquivo não serve", "Esse arquivo não serve", "Escolher outro", Tom.AVISO)
        Estado.DISPONIVEL -> Texto("+", "Não instalado", "Ainda não instalado", "Instalar", Tom.NEUTRO)
        Estado.NAO_RODA -> Texto("×", "Não roda aqui", "Não roda neste aparelho", "Instalar mesmo assim", Tom.RUIM)
    }
}
