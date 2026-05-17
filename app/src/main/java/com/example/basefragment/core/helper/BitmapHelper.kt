package com.example.basefragment.core.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.createBitmap

object BitmapHelper {
    fun getBitmapFromEditText(view: View): Bitmap {
        val backgroundDrawable = view.background

        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        if (backgroundDrawable != null) {
            backgroundDrawable.setBounds(0, 0, canvas.width, canvas.height)
            backgroundDrawable.draw(canvas)
        }
        return bitmap
    }
    fun Bitmap.roundTopCorners(radius: Float): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Vẽ toàn bộ với bo 4 góc
        canvas.drawRoundRect(rect, radius, radius, paint)

        // Vẽ đè 2 góc dưới thành vuông
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawRect(
            0f,
            height / 2f,  // từ giữa xuống dưới → vuông
            width.toFloat(),
            height.toFloat(),
            paint
        )

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, 0f, 0f, paint)

        return output
    }
}