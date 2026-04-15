package com.example.basefragment.core.listener.listenerdraw

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.annotation.IntDef
import androidx.core.graphics.toColorInt
import com.example.basefragment.core.custom.drawview.DrawView
import com.example.basefragment.utils.key.DrawKey
import com.pfp.ocmaker.create.maker.data.model.draw.DrawableDraw

class BitmapDrawIcon(drawable: Drawable?, @Gravity gravity: Int) : DrawableDraw(drawable!!, "nbhieu"),
    DrawEvent {
    @IntDef(*[DrawKey.TOP_LEFT, DrawKey.RIGHT_TOP, DrawKey.LEFT_BOTTOM, DrawKey.RIGHT_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Gravity

    var radius = DrawKey.DEFAULT_RADIUS
    var x = 0f
    var y = 0f

    @get:Gravity
    @Gravity
    var positionDefault = DrawKey.TOP_LEFT
    var event: DrawEvent? = null

    init {
        positionDefault = gravity
    }

    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionDown(tattooView, event)
        }
    }

    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionMove(tattooView, event)
        }
    }

    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionUp(tattooView, event)
        }
    }


    fun draw(canvas: Canvas, paint: Paint) {

        val gradient = LinearGradient(
            x - radius, y,      // BÊN TRÁI
            x + radius, y,      // BÊN PHẢI
            intArrayOf(
                "#7EFEFD".toColorInt(), // màu trái
                "#F5A8F6".toColorInt()  // màu phải
            ),
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        canvas.drawCircle(x, y, radius, paint)

        paint.shader = null // reset để tránh ảnh hưởng super.draw
        super.draw(canvas)
    }
}

