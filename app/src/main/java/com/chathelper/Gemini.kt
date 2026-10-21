package com.chathelper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    const val MODEL = "gemini-2.5-flash"

    // returns Pair(reply, tip)
    fun reply(c: Context, sender: String, msg: String): Pair<String, String>? {
        val p = Store.prefs(c)
        val key = p.getString("key", "") ?: ""
        if (key.isBlank()) return null
        val mode = p.getString("mode", "Friends")
        val gender = p.getString("gender", "Male")
        val style = p.getString("style", "")
        val tips = p.getBoolean("tips", true)

        var sys = "Tum user ke liye uske dost ko bhejne wale reply ka draft likhte ho. " +
            "User ka gender: $gender. Mode: $mode " +
            "(Friends = casual dosti, Impress = respectful, confident, thoda charming, pushy nahi, " +
            "Love = pyaar bhara aur caring). " +
            "User ke bolne ka style, uske purane messages: $style. " +
            "Rules: sirf ek chhota natural reply likho, jaisa user khud likhta, usi language aur style mein. " +
            "Jhoothi baatein ya jhoothe wade mat karo, pushy ya manipulative mat bano. "
        if (tips) sys += "Reply ke baad nayi line mein 'TIP:' likho aur ek chhota tip do " +
            "jisse user reply mein apni real baat jod sake."

        return try {
            val body = JSONObject()
                .put("systemInstruction", JSONObject().put("parts",
                    JSONArray().put(JSONObject().put("text", sys))))
                .put("contents", JSONArray().put(JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject()
                        .put("text", "Dost ($sender) ka message: $msg")))))
            val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val resp = conn.inputStream.bufferedReader().readText()
            val text = JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            val parts = text.split("TIP:")
            Pair(parts[0].trim(), if (parts.size > 1) parts[1].trim() else "")
        } catch (e: Exception) {
            null
        }
    }
}
