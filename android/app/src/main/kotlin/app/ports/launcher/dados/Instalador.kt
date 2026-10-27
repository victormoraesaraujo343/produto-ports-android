package app.ports.launcher.dados

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import java.io.IOException

/** Baixa o APK e entrega ao PackageInstaller. O resultado chega no ReceptorInstalacao. */
class Instalador(private val contexto: Context) {
    class Pacote(val nome: String, val versao: String?)

    private val pm = contexto.packageManager

    fun podeInstalar(): Boolean = pm.canRequestPackageInstalls()

    /** Tela de Ajustes em que o jogador libera "instalar apps desconhecidos" pro launcher. */
    fun intentPermissao(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${contexto.packageName}"))

    /** Baixa pra `cacheDir/apks/<id>.apk`. `progresso` recebe 0–100. Bloqueante: chamar em IO. */
    fun baixar(id: String, url: String, progresso: (Int) -> Unit): File {
        val pasta = File(contexto.cacheDir, "apks").apply { mkdirs() }
        val destino = File(pasta, "$id.apk")
        val parcial = File(pasta, "$id.apk.parcial")
        parcial.delete()
        Rede.baixar(url, parcial, progresso)
        destino.delete()
        if (!parcial.renameTo(destino)) throw IOException("não consegui guardar o APK")
        return destino
    }

    /** Lê o nome do pacote e a versão do APK, sem instalar. */
    fun lerPacote(arquivo: File): Pacote? {
        val info = pm.getPackageArchiveInfo(arquivo.absolutePath, 0) ?: return null
        val nome = info.packageName ?: return null
        return Pacote(nome, info.versionName)
    }

    /** Cria a sessão, grava o APK e confirma. Quem recebe o resultado é o ReceptorInstalacao. */
    fun enviar(arquivo: File, pacote: String, portId: String) {
        val instalador = pm.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(pacote) }
        val sessaoId = instalador.createSession(params)
        val sessao = instalador.openSession(sessaoId)
        try {
            sessao.openWrite("base.apk", 0, arquivo.length()).use { saida ->
                arquivo.inputStream().use { it.copyTo(saida) }
                sessao.fsync(saida)
            }
            val intent = Intent(contexto, ReceptorInstalacao::class.java).putExtra(EXTRA_PORT_ID, portId)
            // o sistema precisa completar o intent com os extras do resultado
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pendente = PendingIntent.getBroadcast(contexto, sessaoId, intent, flags)
            sessao.commit(pendente.intentSender)
        } catch (e: Exception) {
            sessao.abandon()
            throw e
        } finally {
            sessao.close()
        }
    }

    companion object {
        const val EXTRA_PORT_ID = "app.ports.launcher.EXTRA_PORT_ID"
    }
}
