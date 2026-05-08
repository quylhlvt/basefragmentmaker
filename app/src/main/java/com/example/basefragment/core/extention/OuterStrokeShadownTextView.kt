package com.example.basefragment.core.extention

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Join
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.example.basefragment.R
import ir.kotlin.kavehcolorpicker.dp

class OuterStrokeShadownTextView : AppCompatTextView {

    private var outerStrokeWidth = 0f
    private var outerStrokeColor: Int = Color.WHITE
    private var outerStrokeJoin: Join = Join.ROUND
    private var strokeMiter = 5f
    private var extraPadding = 0

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs == null) return

        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.OuterStrokeTextView
        )

        try {
            outerStrokeWidth = a.getDimension(
                R.styleable.OuterStrokeTextView_outerStrokeWidth,
                0f
            )

            outerStrokeColor = a.getColor(
                R.styleable.OuterStrokeTextView_outerStrokeColor,
                Color.WHITE
            )

            outerStrokeJoin = when (a.getInt(
                R.styleable.OuterStrokeTextView_outerStrokeJoinStyle, 2)) {
                0 -> Join.MITER
                1 -> Join.BEVEL
                2 -> Join.ROUND
                else -> Join.ROUND
            }
        } finally {
            a.recycle()
        }
        if (outerStrokeWidth > 0f) {
            extraPadding = (outerStrokeWidth * dp(2)).toInt()
        }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // ✅ Apply sau khi XML padding đã được set xong
        if (extraPadding > 0) {
            setPadding(
                paddingLeft + extraPadding,
                paddingTop ,
                paddingRight + extraPadding,
                paddingBottom
            )
            extraPadding = 0  // reset tránh apply 2 lần
        }
    }
    override fun onDraw(canvas: Canvas) {
        if (outerStrokeWidth > 0f) {
            val paint = paint
            val originalColor = paint.color
            val originalStyle = paint.style
            val originalStrokeWidth = paint.strokeWidth
            val originalJoin = paint.strokeJoin

            paint.clearShadowLayer()
            paint.color = outerStrokeColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = outerStrokeWidth
            paint.strokeJoin = outerStrokeJoin
            paint.strokeMiter = strokeMiter
            paint.isAntiAlias = true

            // Lưu ellipsize để tắt marquee tạm thời
            val ellipsize = ellipsize
            setEllipsize(null)
            super.onDraw(canvas)
            setEllipsize(ellipsize)

            paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
            paint.color = originalColor
            paint.style = originalStyle
            paint.strokeWidth = originalStrokeWidth
            paint.strokeJoin = originalJoin
            super.onDraw(canvas)
        } else {
            super.onDraw(canvas)
        }
    }
}