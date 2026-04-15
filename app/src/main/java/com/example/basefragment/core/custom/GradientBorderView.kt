package com.example.basefragment.core.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class GradientBorderView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 9f // 3dp
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val gradient = LinearGradient(
            0f, h / 2f, w.toFloat(), h / 2f,
            intArrayOf(Color.parseColor("#667eea"), Color.parseColor("#764ba2")),
            null, Shader.TileMode.CLAMP
        )
        paint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        val rect = RectF(4.5f, 4.5f, width - 4.5f, height - 4.5f)
        canvas.drawRoundRect(rect, 36f, 36f, paint)
    }
}