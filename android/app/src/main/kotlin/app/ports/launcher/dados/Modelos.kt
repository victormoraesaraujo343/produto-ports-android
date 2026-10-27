package app.ports.launcher.dados

import android.content.Intent
import app.ports.core.PortNaTela

/** O que a tela de catálogo recebe do Repositorio. */
sealed interface EstadoCatalogo {
    data object Carregando : EstadoCatalogo

    /** `doCache`: a lista veio do arquivo salvo (sem rede agora ou ainda válido). */
    data class Pronto(val ports: List<PortNaTela>, val atualizadoEm: String?, val doCache: Boolean) : EstadoCatalogo

    data class Erro(val mensagem: String) : EstadoCatalogo
}

sealed interface ResultadoInstalacao {
    /** O download acabou e a instalação foi entregue ao sistema. O resultado chega depois. */
    data object Iniciado : ResultadoInstalacao

    /** Falta liberar "instalar apps desconhecidos" pro app. `intent` abre essa tela. */
    data class PrecisaPermissao(val intent: Intent) : ResultadoInstalacao

    data class Falhou(val mensagem: String) : ResultadoInstalacao
}

/** Uma release do GitHub, com o APK que bate com o padrão do port. */
data class Release(val tag: String, val apkUrl: String, val tamanhoBytes: Long, val publicadoEm: String?)
