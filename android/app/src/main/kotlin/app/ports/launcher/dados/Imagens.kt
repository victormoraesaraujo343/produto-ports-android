package app.ports.launcher.dados

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Carregador de imagens das capas, sem dependência nova: baixa pra
 * `cacheDir/imagens/<sha1>`, decodifica reduzido e guarda em memória (LRU).
 */
class Imagens(contexto: Context) {
    private val pasta = File(contexto.cacheDir, "imagens").apply { mkdirs() }
    private val memoria = object : LruCache<String, Bitmap>(((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt()) {
        override fun sizeOf(chave: String, valor: Bitmap): Int = valor.byteCount / 1024
    }

    /** Imagem com largura de no máximo `larguraMax` pixels, ou nulo se não deu. */
    suspend fun carregar(url: String, larguraMax: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val chave = "$url@$larguraMax"
        memoria.get(chave)?.let { return@withContext it.asImageBitmap() }
        val arquivo = arquivoDe(url)
        if (!arquivo.exists() && !baixar(url, arquivo)) return@withContext null
        val bitmap = decodificar(arquivo, larguraMax) ?: run {
            // arquivo que não é imagem: apaga pra tentar de novo depois
            arquivo.delete()
            return@withContext null
        }
        memoria.put(chave, bitmap)
        bitmap.asImageBitmap()
    }

    private fun baixar(url: String, destino: File): Boolean = try {
        val parcial = File(destino.path + ".parcial")
        Rede.baixar(url, parcial)
        parcial.renameTo(destino)
    } catch (_: IOException) {
        false
    }

    private fun decodificar(arquivo: File, larguraMax: Int): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(arquivo.path, medidas)
        if (medidas.outWidth <= 0) return null
        val alvo = larguraMax.coerceAtLeast(1)
        var amostra = 1
        while (medidas.outWidth / (amostra * 2) >= alvo) amostra *= 2
        return BitmapFactory.decodeFile(arquivo.path, BitmapFactory.Options().apply { inSampleSize = amostra })
    }

    private fun arquivoDe(url: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(pasta, hash)
    }
}
