package com.oc.catemoji.catoc.core.extention

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.oc.catemoji.catoc.R

class CurvedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPath = Path()
    private val oval = RectF()
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)

    private var text = ""
    private var textSize = 28f * resources.displayMetrics.scaledDensity
    private var textColor = Color.WHITE
    private var curveDepth = 0.12f
    private var centerAngle = -90f
    private var strokeWidth = 0f
    private var strokeColor = Color.BLACK
    private var fontFamily: Typeface? = null

    // Marquee
    private var scrollOffset = 0f
    private var arcLength = 0f
    private var textWidth = 0f
    private var isMarqueeRunning = false
    private val marqueeSpeed = 1.5f  // px/frame, tăng để nhanh hơn
    private val handler = Handler(Looper.getMainLooper())
    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (!isMarqueeRunning) return
            scrollOffset += marqueeSpeed
            // Reset sau khi text + gap chạy xong 1 vòng
            val gap =  0.5f  // khoảng cách giữa 2 lần lặp
            if (scrollOffset > textWidth + gap) {
                scrollOffset = 0f
            }
            invalidate()
            handler.postDelayed(this, 8)
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.CurvedTextView)
            text         = a.getString(R.styleable.CurvedTextView_android_text) ?: ""
            textSize     = a.getDimension(R.styleable.CurvedTextView_android_textSize, textSize)
            textColor    = a.getColor(R.styleable.CurvedTextView_android_textColor, Color.WHITE)
            curveDepth   = a.getFloat(R.styleable.CurvedTextView_curveDepth, 0.12f)
            centerAngle  = a.getFloat(R.styleable.CurvedTextView_arc_centerAngle2, -90f)
            strokeWidth  = a.getDimension(R.styleable.CurvedTextView_arc_strokeWidth, 0f)
            strokeColor  = a.getColor(R.styleable.CurvedTextView_arc_strokeColor, Color.BLACK)
            val fontRes  = a.getResourceId(R.styleable.CurvedTextView_android_fontFamily, -1)
            if (fontRes != -1) fontFamily = ResourcesCompat.getFont(context, fontRes)
            a.recycle()
        }
        paintText.apply {
            this.textSize = this@CurvedTextView.textSize
            color         = textColor
            typeface      = fontFamily
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Ribbon: viewport 314, dải từ x=36→283
        // Tỉ lệ padding: left=36/314, right=(314-283)/314
        val paddingLeftRatio  = 36f / 314f   // ~11.5%
        val paddingRightRatio = 31f / 314f   // ~9.9%
        clipLeft  = w * paddingLeftRatio
        clipRight = w * (1f - paddingRightRatio)

        buildArcPath(w.toFloat(), h.toFloat())
        checkMarquee()
    }

    private var clipLeft  = 0f
    private var clipRight = 0f

    private fun buildArcPath(w: Float, h: Float) {
        val rise   = w * curveDepth
        val radius = (w * w) / (8f * rise) + rise / 2f
        val cx     = w / 2f
        val arcCy  = h + radius - rise- rise - rise/2

        textWidth = paintText.measureText(text)
        val circumference = (2 * Math.PI * radius).toFloat()
        val textAngle     = textWidth * 360f / circumference
        val startAngle    = centerAngle

        arcLength = w
        oval.set(cx - radius, arcCy - radius, cx + radius, arcCy + radius)

        textPath.reset()
        // ✅ Sweep 4x để đủ chỗ cho 2 lần text chạy
        textPath.addArc(oval, startAngle - textAngle * 2f, textAngle * 5f)
    }

    private fun checkMarquee() {
        val ribbonWidth = clipRight - clipLeft
        val needsMarquee = textWidth > ribbonWidth * 0.9f
        if (needsMarquee && !isMarqueeRunning) {
            isMarqueeRunning = true
            scrollOffset = 0f  // ✅ bắt đầu từ vị trí giữa bình thường
            handler.post(marqueeRunnable)
        } else if (!needsMarquee) {
            isMarqueeRunning = false
            scrollOffset = 0f
            handler.removeCallbacks(marqueeRunnable)
        }
    }
    override fun onDraw(canvas: Canvas) {
        if (text.isEmpty()) return

        val fm      = paintText.fontMetrics
        val vOffset = -(fm.ascent + fm.descent) / 2f

        canvas.save()
        canvas.clipRect(clipLeft, 0f, clipRight, height.toFloat())

        val gap = textWidth * 0.5f  // khoảng cách giữa 2 lần text

        // Vẽ stroke
        if (strokeWidth > 0f) {
            paintText.apply {
                style            = Paint.Style.STROKE
                this.strokeWidth = this@CurvedTextView.strokeWidth
                strokeJoin       = Paint.Join.ROUND
                color            = strokeColor
            }
            // ✅ Vẽ text lần 1
            canvas.drawTextOnPath(text, textPath, -scrollOffset, vOffset, paintText)
            // ✅ Vẽ text lần 2 ngay sau lần 1 (offset = textWidth + gap)
            canvas.drawTextOnPath(text, textPath, -(scrollOffset - textWidth - gap), vOffset, paintText)
        }

        // Vẽ fill
        paintText.apply {
            style = Paint.Style.FILL
            color = textColor
        }
        canvas.drawTextOnPath(text, textPath, -scrollOffset, vOffset, paintText)
        canvas.drawTextOnPath(text, textPath, -(scrollOffset - textWidth - gap), vOffset, paintText)

        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkMarquee()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isMarqueeRunning = false
        handler.removeCallbacks(marqueeRunnable)
    }

    fun setText(text: String) {
        this.text = text
        paintText.textSize = textSize
        invalidate()
        post { checkMarquee() }
    }

    fun setTypeface(tf: Typeface?) {
        fontFamily = tf
        paintText.typeface = tf
        invalidate()
    }
}