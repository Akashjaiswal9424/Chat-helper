package com.chathelper

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SendReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", 0)
        val d = Store.drafts[id] ?: return
        val ok = Store.send(c, d, d.reply)
        (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
        Toast.makeText(c, if (ok) "Bhej diya" else "Send nahi hua", Toast.LENGTH_SHORT).show()
    }
}
