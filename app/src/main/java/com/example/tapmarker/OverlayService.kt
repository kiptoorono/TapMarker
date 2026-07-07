package com.example.tapmarker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var controlPanel: View

    private val tapPoints = mutableListOf<TapPoint>()
    private val scrollPoints = mutableListOf<ScrollPoint>()
    private var nextTapId = 1

    data class TapPoint(val id: Int, var x: Int, var y: Int, val view: View)
    data class ScrollPoint(val id: Int, var x: Int, var y: Int, val view: View, val type: String)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("tapmarker_channel", "TapMarker", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(this, "tapmarker_channel")
                .setContentTitle("TapMarker")
                .setContentText("Floating panel active")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build()
            startForeground(1, notification)
        }

        showControlPanel()
    }

    private fun showControlPanel() {
        val inflater = LayoutInflater.from(this)
        controlPanel = inflater.inflate(R.layout.control_panel, null)

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
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        controlPanel.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    params.x = params.x.toInt() - event.rawX.toInt()
                    params.y = params.y.toInt() - event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = event.rawX.toInt() + params.x.toInt()
                    params.y = event.rawY.toInt() + params.y.toInt()
                    windowManager.updateViewLayout(controlPanel, params)
                }
            }
            false
        }

        windowManager.addView(controlPanel, params)

        controlPanel.findViewById<Button>(R.id.btn_add_tap).setOnClickListener { addTapPoint() }
        controlPanel.findViewById<Button>(R.id.btn_add_scroll).setOnClickListener { addScrollPoints() }
        controlPanel.findViewById<Button>(R.id.btn_play).setOnClickListener { printCoordinatesToLog() }
        controlPanel.findViewById<Button>(R.id.btn_clear).setOnClickListener { clearAll() }

        updateCounterDisplay()
    }

    private fun addTapPoint() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.tap_overlay, null)
        val textView = view.findViewById<TextView>(R.id.tv_tap_number)
        val id = nextTapId++
        textView.text = id.toString()

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
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 300 + (tapPoints.size * 40)
        params.y = 500 + (tapPoints.size * 40)

        windowManager.addView(view, params)
        val point = TapPoint(id, params.x, params.y, view)
        tapPoints.add(point)

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    params.x = params.x.toInt() - event.rawX.toInt()
                    params.y = params.y.toInt() - event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = event.rawX.toInt() + params.x.toInt()
                    params.y = event.rawY.toInt() + params.y.toInt()
                    windowManager.updateViewLayout(view, params)
                    point.x = params.x
                    point.y = params.y
                }
            }
            false
        }

        updateCounterDisplay()
        Toast.makeText(this, "Tap #$id added. Drag it to the button.", Toast.LENGTH_SHORT).show()
    }

    private fun addScrollPoints() {
        removeScrollPoints()
        val inflater = LayoutInflater.from(this)

        val startView = inflater.inflate(R.layout.scroll_overlay, null)
        val startLabel = startView.findViewById<TextView>(R.id.tv_scroll_label)
        startLabel.text = "S"
        startLabel.setBackgroundResource(R.drawable.circle_green)
        val startParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        startParams.gravity = Gravity.TOP or Gravity.START
        startParams.x = 400
        startParams.y = 900
        windowManager.addView(startView, startParams)
        val startPoint = ScrollPoint(0, startParams.x, startParams.y, startView, "start")
        scrollPoints.add(startPoint)

        val endView = inflater.inflate(R.layout.scroll_overlay, null)
        val endLabel = endView.findViewById<TextView>(R.id.tv_scroll_label)
        endLabel.text = "E"
        endLabel.setBackgroundResource(R.drawable.circle_blue)
        val endParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        endParams.gravity = Gravity.TOP or Gravity.START
        endParams.x = 400
        endParams.y = 300
        windowManager.addView(endView, endParams)
        val endPoint = ScrollPoint(1, endParams.x, endParams.y, endView, "end")
        scrollPoints.add(endPoint)

        startView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startParams.x = startParams.x.toInt() - event.rawX.toInt()
                    startParams.y = startParams.y.toInt() - event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    startParams.x = event.rawX.toInt() + startParams.x.toInt()
                    startParams.y = event.rawY.toInt() + startParams.y.toInt()
                    windowManager.updateViewLayout(startView, startParams)
                    startPoint.x = startParams.x
                    startPoint.y = startParams.y
                }
            }
            false
        }
        endView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    endParams.x = endParams.x.toInt() - event.rawX.toInt()
                    endParams.y = endParams.y.toInt() - event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    endParams.x = event.rawX.toInt() + endParams.x.toInt()
                    endParams.y = event.rawY.toInt() + endParams.y.toInt()
                    windowManager.updateViewLayout(endView, endParams)
                    endPoint.x = endParams.x
                    endPoint.y = endParams.y
                }
            }
            false
        }

        updateCounterDisplay()
        Toast.makeText(this, "Drag S (Start) and E (End) for scroll.", Toast.LENGTH_SHORT).show()
    }

    private fun removeScrollPoints() {
        scrollPoints.forEach { try { windowManager.removeView(it.view) } catch (e: Exception) {} }
        scrollPoints.clear()
        updateCounterDisplay()
    }

    private fun clearAll() {
        tapPoints.forEach { try { windowManager.removeView(it.view) } catch (e: Exception) {} }
        tapPoints.clear()
        nextTapId = 1
        removeScrollPoints()
        updateCounterDisplay()
        Toast.makeText(this, "Cleared all points.", Toast.LENGTH_SHORT).show()
    }

    private fun printCoordinatesToLog() {
        if (tapPoints.isEmpty() && scrollPoints.size < 2) {
            Toast.makeText(this, "Add taps and a scroll first!", Toast.LENGTH_SHORT).show()
            return
        }

        val coords = StringBuilder()

        if (tapPoints.isNotEmpty()) {
            coords.append("TAP:")
            val sorted = tapPoints.sortedBy { it.id }
            coords.append(sorted.joinToString(";") { "${it.id},${it.x},${it.y}" })
        }

        if (scrollPoints.size == 2) {
            if (coords.isNotEmpty()) coords.append("|")
            coords.append("SCROLL:")
            val start = scrollPoints.find { it.type == "start" }
            val end = scrollPoints.find { it.type == "end" }
            if (start != null && end != null) {
                coords.append("${start.x},${start.y},${end.x},${end.y}")
            }
        }

        val finalString = coords.toString()
        Log.i("TAP_MARKER", "COORDS: $finalString")
        Toast.makeText(this, "Coordinates sent to laptop! Check ADB logs.", Toast.LENGTH_LONG).show()
        println("TAP_MARKER_COORDS: $finalString")
    }

    private fun updateCounterDisplay() {
        val tv = controlPanel.findViewById<TextView>(R.id.tv_point_count)
        val tapCount = tapPoints.size
        val scrollStatus = if (scrollPoints.size == 2) "Yes" else "No"
        tv.text = "Taps: $tapCount | Scroll: $scrollStatus"
    }

    override fun onDestroy() {
        super.onDestroy()
        try { windowManager.removeView(controlPanel) } catch (e: Exception) {}
        tapPoints.forEach { try { windowManager.removeView(it.view) } catch (e: Exception) {} }
        scrollPoints.forEach { try { windowManager.removeView(it.view) } catch (e: Exception) {} }
    }
}
