package com.vertex.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val minecraftPackage = "com.mojang.minecraftpe"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "VERTEX"
            textSize = 42f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Client for Minecraft Bedrock"
            textSize = 16f
            setTextColor(Color.parseColor("#8B949E"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 64)
        }

        val launchButton = Button(this).apply {
            text = "LAUNCH"
            setOnClickListener { launchClient() }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(launchButton)
        setContentView(layout)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }
    }

    private fun launchClient() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Allow \"Display over other apps\" for Vertex, then tap Launch again",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        val minecraftIntent = packageManager.getLaunchIntentForPackage(minecraftPackage)
        if (minecraftIntent == null) {
            Toast.makeText(this, "Minecraft is not installed", Toast.LENGTH_LONG).show()
            return
        }

        startForegroundService(Intent(this, OverlayService::class.java))
        startActivity(minecraftIntent)
    }
}
