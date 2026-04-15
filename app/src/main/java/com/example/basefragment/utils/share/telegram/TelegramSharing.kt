package com.example.basefragment.utils.share.telegram

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.basefragment.R

object TelegramSharing {
    fun importToTelegram(context: Context, uriList: List<Uri>) {
        val list = ArrayList(uriList)

        // Chỉ cần READ, WRITE gây lỗi SecurityException trên một số thiết bị
        list.forEach { uri ->
            context.grantUriPermission(
                "org.telegram.messenger",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            context.grantUriPermission(
                "org.telegram.messenger.web",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val intent = Intent("org.telegram.messenger.CREATE_STICKER_PACK").apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
            putExtra("IMPORTER", context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/*"
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.no_app_found_to_handle_this_action),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}