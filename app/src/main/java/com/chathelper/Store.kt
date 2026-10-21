package com.chathelper

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject

data class Draft(
    val id: Int, val sender: String, val incoming: String,
    var reply: String, val tip: String, val action: Notification.Action
)

object Store {
    val drafts = LinkedHashMap<Int, Draft>()

    fun prefs(c: Context) = c.getSharedPreferences("ch", Context.MODE_PRIVATE)

    fun send(c: Context, d: Draft, text: String): Boolean {
        return try {
            val inputs = d.action.remoteInputs ?: return false
            val i = Intent()
            val b = Bundle()
            b.putCharSequence(inputs[0].resultKey, text)
            RemoteInput.addResultsToIntent(inputs, i, b)
            d.action.actionIntent.send(c, 0, i)
            addHistory(c, d.sender, d.incoming, text)
            drafts.remove(d.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun addHistory(c: Context, sender: String, incoming: String, out: String) {
        val old = JSONArray(prefs(c).getString("hist", "[]"))
        val arr = JSONArray()
        val start = maxOf(0, old.length() - 99)
        for (k in start until old.length()) arr.put(old.get(k))
        arr.put(JSONObject().put("s", sender).put("in", incoming).put("out", out))
        prefs(c).edit().putString("hist", arr.toString()).apply()
    }

    fun historyText(c: Context): String {
        val arr = JSONArray(prefs(c).getString("hist", "[]"))
        val sb = StringBuilder()
        for (k in arr.length() - 1 downTo 0) {
            val o = arr.getJSONObject(k)
            sb.append(o.getString("s")).append(": ").append(o.getString("in"))
                .append("\nAapne bheja: ").append(o.getString("out")).append("\n\n")
        }
        return if (sb.isEmpty()) "Abhi koi history nahi." else sb.toString()
    }
}
