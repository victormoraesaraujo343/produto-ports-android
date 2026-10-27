package app.ports.launcher.dados

import android.content.Context
import android.content.pm.PackageManager

/**
 * Liga o repo do GitHub ao nome do pacote Android (SharedPreferences `pacotes`),
 * que só se descobre lendo o APK baixado. Também checa se o pacote está instalado.
 */
class Pacotes(private val contexto: Context) {
    private val prefs = contexto.getSharedPreferences("pacotes", Context.MODE_PRIVATE)
    private val pm = contexto.packageManager

    fun pacoteDe(repo: String): String? = prefs.getString(repo, null)

    fun gravar(repo: String, pacote: String) {
        prefs.edit().putString(repo, pacote).apply()
    }

    /** Versão instalada do pacote, ou nulo se ele não está no aparelho. */
    fun versaoInstalada(pacote: String): String? = try {
        pm.getPackageInfo(pacote, 0).versionName ?: "?"
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
