package app.ports.launcher.dados

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** HTTP mínimo com HttpURLConnection: timeout de 15 s e User-Agent do app. */
internal object Rede {
    private const val TIMEOUT_MS = 15_000
    private const val AGENTE = "PortsAndroid"

    class Resposta(val codigo: Int, val corpo: String)

    /** Busca texto. Código de erro não lança exceção: quem chama decide. */
    fun texto(url: String, cabecalhos: Map<String, String> = emptyMap()): Resposta {
        val con = abrir(url, cabecalhos)
        try {
            val codigo = con.responseCode
            val fluxo = if (codigo in 200..299) con.inputStream else con.errorStream
            val corpo = fluxo?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Resposta(codigo, corpo)
        } finally {
            con.disconnect()
        }
    }

    /** Baixa pro `destino`. `progresso` recebe 0–100 quando o tamanho é conhecido. */
    fun baixar(url: String, destino: File, progresso: ((Int) -> Unit)? = null) {
        val con = abrir(url, emptyMap())
        try {
            val codigo = con.responseCode
            if (codigo !in 200..299) throw IOException("HTTP $codigo")
            val total = con.contentLengthLong
            var lido = 0L
            var ultimo = -1
            con.inputStream.use { entrada ->
                destino.outputStream().use { saida ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        saida.write(buffer, 0, n)
                        lido += n
                        if (progresso != null && total > 0) {
                            val pct = (lido * 100 / total).toInt()
                            if (pct != ultimo) {
                                ultimo = pct
                                progresso(pct)
                            }
                        }
                    }
                }
            }
        } finally {
            con.disconnect()
        }
    }

    private fun abrir(url: String, cabecalhos: Map<String, String>): HttpURLConnection {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = TIMEOUT_MS
        con.readTimeout = TIMEOUT_MS
        con.instanceFollowRedirects = true
        con.setRequestProperty("User-Agent", AGENTE)
        cabecalhos.forEach { (k, v) -> con.setRequestProperty(k, v) }
        return con
    }
}
