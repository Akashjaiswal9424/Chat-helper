package com.chathelper

import android.app.*
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class ChatListener : NotificationListenerService() {
    private var last = ""

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp") return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val ex = n.extras
        if (ex.getBoolean("android.isGroupConversation")) return
        val sender = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        if (text.contains("new messages") || text.contains("Checking for new")) return
        val action = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return
        val sig = "$sender|$text"
        if (sig == last) return
        last = sig
        val id = sbn.key.hashCode()

        Thread {
            val r = Gemini.reply(this, sender, text) ?: return@Thread
            Store.drafts[id] = Draft(id, sender, text, r.first, r.second, action)
            show(id, sender, text, r.first, r.second)
        }.start()
    }

    private fun show(id: Int, sender: String, msg: String, reply: String, tip: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("drafts", "Reply drafts", NotificationManager.IMPORTANCE_HIGH)
        )
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val sendPi = PendingIntent.getBroadcast(
            this, id, Intent(this, SendReceiver::class.java).putExtra("id", id), flags
        )
        val openPi = PendingIntent.getActivity(this, id, Intent(this, MainActivity::class.java), flags)
        val big = "Dost: $msg\n\nReply: $reply" + if (tip.isNotEmpty()) "\n\nTip: $tip" else ""
        val nb = NotificationCompat.Builder(this, "drafts")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("$sender ke liye reply ready")
            .setContentText(reply)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setContentIntent(openPi)
            .addAction(0, "Send", sendPi)
            .addAction(0, "Edit", openPi)
            .setAutoCancel(true)
            .build()
        nm.notify(id, nb)
    }
}
