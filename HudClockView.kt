package com.example.smartaudioalarm

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class HudClockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cyanGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00F0FF")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        setShadowLayer(18f, 0f, 0f, Color.parseColor("#00F0FF"))
    }

    private val dashedRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0088AA")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
    }

    private val innerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1400F0FF")
        style = Paint.Style.FILL
    }

    private var angle = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (min(width, height) / 2f) - 25f

        canvas.drawCircle(cx, cy, r * 0.86f, innerFill)
        canvas.drawCircle(cx, cy, r, cyanGlow)

        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.89f, dashedRing)
        canvas.restore()

        val sec = Calendar.getInstance().get(Calendar.SECOND)
        val secAngle = Math.toRadians(((sec * 6f) - 90f).toDouble())
        val dotX = cx + (r * 0.95f) * cos(secAngle).toFloat()
        val dotY = cy + (r * 0.95f) * sin(secAngle).toFloat()

        cyanGlow.style = Paint.Style.FILL
        canvas.drawCircle(dotX, dotY, 9f, cyanGlow)
        cyanGlow.style = Paint.Style.STROKE

        angle = (angle + 0.4f) % 360f
        postInvalidateDelayed(30)
    }
}
