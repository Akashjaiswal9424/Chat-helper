package com.chathelper

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val BG = Color.parseColor("#FFFBFC")
    private val BLUSH = Color.parseColor("#FBEAF0")
    private val LINE = Color.parseColor("#F4C0D1")
    private val ROSE = Color.parseColor("#D4537E")
    private val MID = Color.parseColor("#993556")
    private val DEEP = Color.parseColor("#72243E")

    private lateinit var draftInfo: TextView
    private lateinit var draftEdit: EditText
    private lateinit var sendBtn: TextView
    private lateinit var hist: TextView
    private var mode = "Friends"
    private var gender = "Male"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun label(t: String) = TextView(this).apply {
        text = t; textSize = 12f; setTextColor(MID)
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun field(h: String, value: String, lines: Int) = EditText(this).apply {
        hint = h; setText(value); minLines = lines
        textSize = 14f; setTextColor(DEEP); setHintTextColor(LINE)
        background = shape(Color.WHITE, LINE, 14)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        gravity = Gravity.TOP
    }

    private fun pill(t: String, filled: Boolean) = TextView(this).apply {
        text = t; textSize = 15f; gravity = Gravity.CENTER
        setTextColor(if (filled) Color.WHITE else ROSE)
        background = shape(if (filled) ROSE else Color.WHITE, ROSE, 26)
        setPadding(dp(16), dp(13), dp(16), dp(13))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }
    }

    private fun chipRow(opts: List<String>, current: String, onPick: (String) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val views = ArrayList<TextView>()
        fun paint(sel: String) {
            views.forEachIndexed { i, v ->
                val on = opts[i] == sel
                v.background = shape(if (on) ROSE else Color.WHITE, if (on) ROSE else LINE, 22)
                v.setTextColor(if (on) Color.WHITE else MID)
            }
        }
        opts.forEach { o ->
            val v = TextView(this).apply {
                text = o; textSize = 14f
                setPadding(dp(18), dp(9), dp(18), dp(9))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
            }
            v.setOnClickListener { onPick(o); paint(o) }
            views.add(v); row.addView(v)
        }
        paint(current)
        return row
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(Color.WHITE, LINE, 18)
        setPadding(dp(16), dp(14), dp(16), dp(16))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        window.statusBarColor = BG
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val p = Store.prefs(this)
        mode = p.getString("mode", "Friends") ?: "Friends"
        gender = p.getString("gender", "Male") ?: "Male"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(20), dp(28), dp(20), dp(32))
        }
        root.addView(TextView(this).apply { text = "Hello"; textSize = 13f; setTextColor(MID) })
        root.addView(TextView(this).apply {
            text = "Chat Helper"; textSize = 30f; setTextColor(DEEP)
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        })

        root.addView(label("Aaj ka mood"))
        root.addView(chipRow(listOf("Friends", "Impress", "Love"), mode) { mode = it })
        root.addView(label("Aap"))
        root.addView(chipRow(listOf("Male", "Female"), gender) { gender = it })

        root.addView(label("Aapka style"))
        val style = field("Apne 10-15 purane messages yahan paste karo", p.getString("style", "") ?: "", 4)
        root.addView(style)
        root.addView(label("Gemini API key (zarurat ho tabhi)"))
        val key = field("Khali chhodo agar key app mein judi hai", p.getString("key", "") ?: "", 1)
        root.addView(key)

        val tips = CheckBox(this).apply {
            text = "Tips dikhao"; setTextColor(MID); isChecked = p.getBoolean("tips", true)
        }
        root.addView(tips)

        val save = pill("Save", false)
        save.setOnClickListener {
            p.edit().putString("key", key.text.toString().trim())
                .putString("mode", mode).putString("gender", gender)
                .putString("style", style.text.toString())
                .putBoolean("tips", tips.isChecked).apply()
            Toast.makeText(this, "Save ho gaya", Toast.LENGTH_SHORT).show()
        }
        root.addView(save)
        val access = pill("Notification Access ON karo", false)
        access.setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        root.addView(access)

        root.addView(label("Naya reply"))
        val dc = card()
        dc.background = shape(BLUSH, LINE, 18)
        draftInfo = TextView(this).apply { textSize = 13f; setTextColor(MID) }
        draftEdit = EditText(this).apply {
            textSize = 15f; setTextColor(DEEP)
            background = shape(Color.WHITE, LINE, 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        draftEdit.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }
        sendBtn = pill("Send", true)
        sendBtn.setOnClickListener {
            val d = Store.drafts.values.lastOrNull() ?: return@setOnClickListener
            val ok = Store.send(this, d, draftEdit.text.toString())
            Toast.makeText(this, if (ok) "Bhej diya" else "Send nahi hua", Toast.LENGTH_SHORT).show()
            refresh()
        }
        dc.addView(draftInfo); dc.addView(draftEdit); dc.addView(sendBtn)
        root.addView(dc)

        root.addView(label("History"))
        val hc = card()
        hist = TextView(this).apply { textSize = 13f; setTextColor(DEEP) }
        hc.addView(hist)
        root.addView(hc)

        setContentView(ScrollView(this).apply { setBackgroundColor(BG); addView(root) })
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
            sendBtn.alpha = 0.4f
        } else {
            draftInfo.text = "${d.sender}: ${d.incoming}" + if (d.tip.isNotEmpty()) "\nTip: ${d.tip}" else ""
            if (draftEdit.text.isEmpty()) draftEdit.setText(d.reply)
            draftEdit.isEnabled = true
            sendBtn.isEnabled = true
            sendBtn.alpha = 1f
        }
        hist.text = Store.historyText(this)
    }
}
