package app.ports.core

/**
 * Uma entrada do catálogo, já no formato do app. Espelha o `ports-catalogue`
 * mais os campos que ele ainda não tem (ARCHITECTURE.md): `pacote` (nome do
 * pacote Android do port) e as cores que fazem as vezes de arte no esqueleto.
 */
data class Port(
    val id: String,
    val titulo: String,
    val logo: String,
    val nomePort: String,
    val tipo: String,
    val plataforma: String,
    val repo: String,
    val descricao: String,
    val arquivoQueServe: String,
    val tamanho: String,
    val versaoMaisNova: String?,
    val pacote: String?,
    val cores: List<Long>?,
    val rodaAqui: Boolean = true,
    val motivoNaoRoda: String? = null,
    /** Só no catálogo de exemplo: o estado que o lab mostra pra esse port. */
    val estadoDeExemplo: Estado? = null,
    val progressoDeExemplo: Int? = null,
    val jogadoHa: String? = null,
    /** Capa e fundo vindos do catálogo online (ou do SteamGridDB, depois). */
    val capaUrl: String? = null,
    val heroUrl: String? = null,
    /** Regex do APK no GitHub (`assets.android.pattern`). */
    val padraoApk: String? = null,
    /** Canal das releases: `latest` ou `prerelease`. */
    val canal: String = "latest",
    val estrelas: Int? = null,
    /** Feito com IA (`aiFlag` do catálogo, marcado pelo awesome-decomp-games). Ausente é falso. */
    val aiFlag: Boolean = false,
    /** Testado no catálogo (`tested`) ou curado (`source == "curated"`). */
    val testado: Boolean = false,
    /** Data (ISO) da última atividade: `stats.pushedAt`, trocada pela da release quando ela é conhecida. */
    val atualizadoEm: String? = null,
)

/** O que se lista na tela: o port e o estado já calculado. */
data class PortNaTela(val port: Port, val estado: Estado, val versaoInstalada: String?, val progresso: Int? = null)

/** Ports de VR (Meta Quest) não rodam num portátil: ficam de fora de todas as listas. */
object Vr {
    private val marca = Regex("""\b(VR|Meta Quest|Quest \d)\b""", RegexOption.IGNORE_CASE)

    fun ehVr(titulo: String): Boolean = marca.containsMatchIn(titulo)
}

object Ordem {
    /** A–Z estável (Síntese §2 do Roseta). */
    val alfabetica: Comparator<PortNaTela> = compareBy(java.text.Collator.getInstance(java.util.Locale("pt", "BR"))) { it.port.titulo }
}

enum class Filtro(val nome: String, val aceita: (PortNaTela) -> Boolean) {
    TODOS("Todos", { true }),
    INSTALADOS("Instalados", { it.estado !in setOf(Estado.DISPONIVEL, Estado.NAO_RODA) }),
    DISPONIVEIS("Disponíveis", { it.estado in setOf(Estado.DISPONIVEL, Estado.NAO_RODA) }),
    ATUALIZACOES("Atualizações", { it.estado == Estado.ATUALIZAR || it.estado == Estado.BAIXANDO }),
}
