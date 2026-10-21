package com.chathelper

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var draftInfo: TextView
    private lateinit var draftEdit: EditText
    private lateinit var sendBtn: Button
    private lateinit var hist: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        val p = Store.prefs(this)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        fun label(t: String) = TextView(ctx).apply { text = t; textSize = 16f; setPadding(0, 24, 0, 4) }

        val key = EditText(ctx).apply { hint = "Gemini API key"; setText(p.getString("key", "")) }
        val modes = arrayOf("Friends", "Impress", "Love")
        val genders = arrayOf("Male", "Female")
        val mode = Spinner(ctx)
        mode.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, modes)
        mode.setSelection(maxOf(0, modes.indexOf(p.getString("mode", "Friends"))))
        val gender = Spinner(ctx)
        gender.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, genders)
        gender.setSelection(maxOf(0, genders.indexOf(p.getString("gender", "Male"))))
        val style = EditText(ctx).apply {
            hint = "Apne 10-15 purane messages yahan paste karo"
            minLines = 4
            setText(p.getString("style", ""))
        }
        val tips = CheckBox(ctx).apply { text = "Tips dikhao"; isChecked = p.getBoolean("tips", true) }
        val save = Button(ctx).apply { text = "Save" }
        save.setOnClickListener {
            p.edit().putString("key", key.text.toString().trim())
                .putString("mode", mode.selectedItem.toString())
                .putString("gender", gender.selectedItem.toString())
                .putString("style", style.text.toString())
                .putBoolean("tips", tips.isChecked).apply()
            Toast.makeText(ctx, "Save ho gaya", Toast.LENGTH_SHORT).show()
        }
        val access = Button(ctx).apply { text = "Notification Access ON karo" }
        access.setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

        draftInfo = TextView(ctx)
        draftEdit = EditText(ctx)
        sendBtn = Button(ctx).apply { text = "Send" }
        sendBtn.setOnClickListener {
            val d = Store.drafts.values.lastOrNull() ?: return@setOnClickListener
            val ok = Store.send(ctx, d, draftEdit.text.toString())
            Toast.makeText(ctx, if (ok) "Bhej diya" else "Send nahi hua", Toast.LENGTH_SHORT).show()
            refresh()
        }
        hist = TextView(ctx)

        listOf(
            label("Gemini API key"), key, label("Mode"), mode, label("Gender"), gender,
            label("Aapka style"), style, tips, save, access,
            label("Naya reply"), draftInfo, draftEdit, sendBtn,
            label("History"), hist
        ).forEach { root.addView(it) }
        setContentView(ScrollView(ctx).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val d = Store.drafts.values.lastOrNull()
        if (d == null) {
            draftInfo.text = "Abhi koi naya message nahi."
            draftEdit.setText("")
            draftEdit.isEnabled = false
            sendBtn.isEnabled = false
        } else {
            draftInfo.text = "${d.sender}: ${d.incoming}" + if (d.tip.isNotEmpty()) "\nTip: ${d.tip}" else ""
            if (draftEdit.text.isEmpty()) draftEdit.setText(d.reply)
            draftEdit.isEnabled = true
            sendBtn.isEnabled = true
        }
        hist.text = Store.historyText(this)
    }
}
