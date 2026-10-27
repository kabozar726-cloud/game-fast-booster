package com.gamefast.booster

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One before/after ping entry, entered by the player from PUBG's lobby ms. */
data class BoostEntry(
    val id: Long,
    val beforeMs: Int,
    val afterMs: Int,
    val region: String,
    val note: String,
    val time: Long,
)

/**
 * Local storage for the Before/After log (app-private SharedPreferences).
 * Values are ONLY what the player enters from their own game lobby —
 * the app never invents ms numbers.
 */
object BoostLogStore {
    private const val PREFS = "boost_log"
    private const val KEY = "entries"

    fun load(context: Context): List<BoostEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val out = mutableListOf<BoostEntry>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += BoostEntry(
                id = o.getLong("id"),
                beforeMs = o.getInt("beforeMs"),
                afterMs = o.getInt("afterMs"),
                region = o.optString("region"),
                note = o.optString("note"),
                time = o.getLong("time"),
            )
        }
        return out.sortedByDescending { it.time }
    }

    fun add(context: Context, entry: BoostEntry) {
        val list = load(context).toMutableList()
        list.add(0, entry)
        save(context, list)
    }

    fun delete(context: Context, id: Long) {
        save(context, load(context).filter { it.id != id })
    }

    private fun save(context: Context, list: List<BoostEntry>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("beforeMs", e.beforeMs)
                put("afterMs", e.afterMs)
                put("region", e.region)
                put("note", e.note)
                put("time", e.time)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
