package com.vertex.client

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class RadialMenuView(
    context: Context,
    private val onClose: () -> Unit
) : View(context) {

    private val labels = arrayOf("MODS", "INFO", "SETTINGS", "HUD")
    private val details = arrayOf(
        "FPS  |  CPS  |  Keystrokes\nToggles coming next.",
        "Vertex v0.1.0\nBedrock client for Android.",
        "Bubble size, opacity, theme\nComing soon.",
        "Move and resize your widgets\nComing soon."
    )
    private val centerId = 4
    private var pressed = -1
    private var selected = -1

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#B3FFFFFF")
        strokeWidth = dp(1.5f)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.12f
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#CCFFFFFF")
        textSize = dp(12f)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(dp(300f).toInt(), (dp(300f) + dp(76f)).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = width / 2f
        val rOuter = width / 2f - dp(4f)
        val rInner = rOuter * 0.42f
        val rCenter = rOuter * 0.30f
        val outer = RectF(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter)
        val inner = RectF(cx - rInner, cy - rInner, cx + rInner, cy + rInner)

        for (i in 0..3) {
            val mid = -90f + 90f * i
            val start = mid - 40f
            val path = Path()
            path.arcTo(outer, start, 80f)
            path.arcTo(inner, start + 80f, -80f)
            path.close()

            val isPressed = pressed == i
            fill.color = if (isPressed) Color.WHITE else Color.parseColor("#D9000000")
            canvas.drawPath(path, fill)
            canvas.drawPath(path, stroke)

            val rm = (rOuter + rInner) / 2f
            val rad = Math.toRadians(mid.toDouble())
            val tx = cx + rm * cos(rad).toFloat()
            val ty = cy + rm * sin(rad).toFloat()
            text.textSize = dp(12f)
            text.color = if (isPressed) Color.BLACK else Color.WHITE
            canvas.drawText(labels[i], tx, ty + dp(4f), text)
        }

        fill.color = if (pressed == centerId) Color.WHITE else Color.parseColor("#D9000000")
        canvas.drawCircle(cx, cy, rCenter, fill)
        canvas.drawCircle(cx, cy, rCenter, stroke)
        text.textSize = dp(26f)
        text.color = if (pressed == centerId) Color.BLACK else Color.WHITE
        canvas.drawText("V", cx, cy + dp(9f), text)

        fill.color = Color.parseColor("#D9000000")
        canvas.drawRoundRect(
            RectF(dp(8f), width + dp(6f), width - dp(8f), height - dp(4f)),
            dp(14f), dp(14f), fill
        )
        val lines = if (selected == -1) listOf("Tap an option") else details[selected].split("\n")
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, cx, width + dp(30f) + i * dp(18f), detailPaint)
        }
    }

    private fun hit(x: Float, y: Float): Int {
        val dx = x - width / 2f
        val dy = y - width / 2f
        val d = hypot(dx, dy)
        val rOuter = width / 2f - dp(4f)
        if (d > rOuter) return -1
        if (d <= rOuter * 0.30f) return centerId
        if (d < rOuter * 0.42f) return -1
        val a = Math.toDegrees(atan2(dy, dx).toDouble())
        return when {
            a >= -135 && a < -45 -> 0
            a >= -45 && a < 45 -> 1
            a >= 45 && a < 135 -> 2
            else -> 3
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pressed = hit(e.x, e.y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val h = hit(e.x, e.y)
                pressed = -1
                if (h == centerId) onClose() else if (h in 0..3) selected = h
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = -1
                invalidate()
            }
        }
        return true
    }
}
