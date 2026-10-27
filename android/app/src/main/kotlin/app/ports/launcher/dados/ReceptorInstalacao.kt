package app.ports.launcher.dados

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/**
 * Recebe o resultado do PackageInstaller. Declarado no manifesto como não
 * exportado: só o sistema, pelo PendingIntent, chega aqui.
 */
class ReceptorInstalacao : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val portId = intent.getStringExtra(Instalador.EXTRA_PORT_ID) ?: return
        val repositorio = Repositorio.obter(contexto)
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // o sistema pede o "instalar" do jogador
                extraIntent(intent)?.let { contexto.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> repositorio.aoTerminarInstalacao(portId, null)
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                repositorio.aoTerminarInstalacao(portId, msg ?: "A instalação não foi concluída.")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extraIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        else intent.getParcelableExtra(Intent.EXTRA_INTENT)
}
