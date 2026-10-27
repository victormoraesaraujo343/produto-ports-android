package app.ports.launcher.dados

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONArray.paraObjetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

/** Texto do campo, ou nulo se o campo não existe, é null ou vazio. */
internal fun JSONObject.optTexto(chave: String): String? =
    if (isNull(chave)) null else optString(chave).takeIf { it.isNotEmpty() }
