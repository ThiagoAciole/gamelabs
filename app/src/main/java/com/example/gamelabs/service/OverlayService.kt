package com.example.gamelabs.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import com.example.gamelabs.MainActivity
import com.example.gamelabs.R

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutInflater = LayoutInflater.from(this)
        floatingView = layoutInflater.inflate(R.layout.overlay_button, null)

        val btnClose = floatingView?.findViewById<ImageButton>(R.id.btn_close_emulator)
        btnClose?.alpha = 0.6f // Levemente mais visível

        btnClose?.setOnClickListener {
            // 1. Tenta matar os processos primeiro (enquanto o emulador ainda está em foco)
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val emulators = listOf(
                "org.ppsspp.ppsspp",
                "org.ppsspp.ppssppgold",
                "xyz.aethersx2.android",
                "com.aethersx2.android"
            )

            emulators.forEach { pkg ->
                try {
                    activityManager.killBackgroundProcesses(pkg)
                } catch (e: Exception) { }
            }

            // 2. Volta para o Gamelabs com Flags de "Reset"
            val intent = Intent(this, MainActivity::class.java).apply {
                // FLAG_ACTIVITY_NEW_TASK: Necessário para chamar de um Service
                // FLAG_ACTIVITY_CLEAR_TASK: Remove todas as telas anteriores da pilha (evita o loading infinito)
                // FLAG_ACTIVITY_NO_ANIMATION: Faz a transição ser instantânea
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }

            startActivity(intent)

            // 3. Para o serviço do botão
            stopSelf()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 20
        params.y = 80 // Ajustado levemente para não cobrir menus de status

        windowManager?.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}