package com.vertex.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        showBubble()
    }

    override fun onDestroy() {
        removePanel()
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: String, radiusDp: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun startAsForeground() {
        val channelId = "vertex_overlay"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Vertex overlay", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Vertex is running")
            .setContentText("Tap the V bubble in game")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun showBubble() {
        val size = dp(56)
        bubbleParams = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        val v = TextView(this).apply {
            text = "V"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#7C3AED"))
            }
            setOnTouchListener { view, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = bubbleParams.x
                        startY = bubbleParams.y
                        touchX = e.rawX
                        touchY = e.rawY
                        moved = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - touchX).toInt()
                        val dy = (e.rawY - touchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) moved = true
                        if (moved) {
                            bubbleParams.x = startX + dx
                            bubbleParams.y = startY + dy
                            wm.updateViewLayout(view, bubbleParams)
                            removePanel()
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel()
                        true
                    }
                    else -> false
                }
            }
        }
        bubble = v
        wm.addView(v, bubbleParams)
    }

    private fun togglePanel() {
        if (panel != null) removePanel() else showPanel()
    }

    private fun removePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    private fun makeButton(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 12f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(10), dp(4), dp(10))
        background = rounded("#21262D", 10)
        layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ).apply { setMargins(dp(3), 0, dp(3), 0) }
        setOnClickListener { onClick() }
    }

    private fun showPanel() {
        val screenW = resources.displayMetrics.widthPixels
        val panelW = dp(260)

        val title = TextView(this).apply {
            text = "VERTEX"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(dp(6), 0, 0, dp(8))
        }

        val detail = TextView(this).apply {
            text = "Pick an option above."
            textSize = 13f
            setTextColor(Color.parseColor("#C9D1D9"))
            setPadding(dp(6), dp(12), dp(6), dp(8))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(makeButton("SETTINGS") {
                detail.text = "Settings coming soon:\nbubble size, opacity, theme."
            })
            addView(makeButton("INFO") {
                detail.text = "Vertex v0.1.0\nBedrock client for Android."
            })
            addView(makeButton("MODULES") {
                detail.text = "FPS: off\nCPS: off\nKeystrokes: off\n\nToggles coming next."
            })
        }

        val quit = TextView(this).apply {
            text = "CLOSE VERTEX"
            textSize = 12f
            setTextColor(Color.parseColor("#F85149"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
            setOnClickListener { stopSelf() }
        }

        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded("#0D1117", 16)
            setPadding(dp(10), dp(12), dp(10), dp(10))
            addView(title)
            addView(row)
            addView(detail)
            addView(quit)
        }

        val params = WindowManager.LayoutParams(
            panelW,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (bubbleParams.x > screenW / 2) bubbleParams.x - panelW - dp(8)
                else bubbleParams.x + dp(64)
            y = bubbleParams.y
        }

        panel = p
        wm.addView(p, params)
    }
}
