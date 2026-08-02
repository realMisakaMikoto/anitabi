package cn.anitabi.navigator.navigation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import cn.anitabi.navigator.MainActivity
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TourPlan
import kotlin.math.roundToInt

internal class TransitOverlayController(
    private val context: Context,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    val isShowing: Boolean
        get() = root != null

    fun render(plan: TourPlan, progress: NavigationProgress, targetDistanceMeters: Double?) {
        if (
            !NavigationControlAvailability.overlayVisible(context) ||
            progress.isPaused ||
            progress.state in setOf(NavigationState.COMPLETED, NavigationState.ENDED)
        ) {
            remove()
            return
        }
        val view = root ?: createRoot().also { created ->
            root = created
            val params = createLayoutParams()
            layoutParams = params
            runCatching { windowManager.addView(created, params) }
                .onFailure {
                    root = null
                    layoutParams = null
                }
        }
        if (root !== view) return
        populate(view, plan, progress, targetDistanceMeters)
    }

    fun remove() {
        val view = root ?: return
        root = null
        layoutParams = null
        runCatching { windowManager.removeViewImmediate(view) }
    }

    private fun createRoot(): LinearLayout {
        val density = context.resources.displayMetrics.density
        val padding = (12 * density).roundToInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable().apply {
                setColor(Color.argb(245, 255, 255, 255))
                cornerRadius = 14 * density
                setStroke((1 * density).roundToInt().coerceAtLeast(1), Color.rgb(210, 207, 199))
            }
            elevation = 10 * density
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        (300 * context.resources.displayMetrics.density).roundToInt(),
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = (12 * context.resources.displayMetrics.density).roundToInt()
        y = (96 * context.resources.displayMetrics.density).roundToInt()
    }

    private fun populate(
        container: LinearLayout,
        plan: TourPlan,
        progress: NavigationProgress,
        targetDistanceMeters: Double?,
    ) {
        container.removeAllViews()
        val leg = plan.legs.getOrNull(progress.legIndex)
        val targetName = leg?.destinationPointId?.let { id ->
            plan.selectedPoints.firstOrNull { it.id == id }?.name
        } ?: "返回起点"
        val title = textView("当前目标：$targetName", 16f, bold = true)
        container.addView(title)
        installDragHandle(title)
        container.addView(textView("第 ${progress.legIndex + 1}/${plan.legs.size.coerceAtLeast(1)} 段", 14f))
        val distance = targetDistanceMeters?.let(::formatDistance) ?: "等待定位"
        container.addView(textView("直线距离：$distance", 14f))
        container.addView(textView(stateLabel(progress), 14f))

        when (progress.state) {
            NavigationState.NAVIGATING -> addButton(container, "打开本段") {
                openHandoff(TransitHandoffActivity.MODE_OPEN, plan.id, progress.legIndex)
            }
            NavigationState.ARRIVING -> addButton(container, "确认到达") {
                openHandoff(TransitHandoffActivity.MODE_CONFIRM_ARRIVAL, plan.id, progress.legIndex)
            }
            NavigationState.NEXT_STOP -> addButton(container, "开始下一段") {
                openHandoff(TransitHandoffActivity.MODE_NEXT, plan.id, progress.legIndex)
            }
            else -> Unit
        }
        addButton(container, "暂停") {
            context.startService(
                Intent(context, NavigationService::class.java).setAction(NavigationService.ACTION_PAUSE_EXTERNAL),
            )
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
        addButton(container, "返回应用") {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
        addButton(container, "结束") {
            openHandoff(TransitHandoffActivity.MODE_END, plan.id, progress.legIndex)
        }
    }

    private fun stateLabel(progress: NavigationProgress): String = when {
        progress.isPaused -> "已暂停"
        progress.state == NavigationState.ARRIVING -> "已接近目标，请确认到达"
        progress.state == NavigationState.DWELLING -> "停留中"
        progress.state == NavigationState.NEXT_STOP -> "停留结束，等待手动开始下一段"
        else -> "路线、班次和换乘由 Google 地图提供"
    }

    private fun textView(text: String, size: Float, bold: Boolean = false): TextView = TextView(context).apply {
        this.text = text
        textSize = size
        setTextColor(Color.rgb(35, 34, 31))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 4, 0, 4)
    }

    private fun addButton(parent: LinearLayout, label: String, onClick: () -> Unit) {
        parent.addView(Button(context).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
        })
    }

    private fun installDragHandle(handle: View) {
        var touchX = 0f
        var touchY = 0f
        var startX = 0
        var startY = 0
        handle.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchX = event.rawX
                    touchY = event.rawY
                    startX = params.x
                    startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (startX - (event.rawX - touchX)).roundToInt()
                    params.y = (startY + (event.rawY - touchY)).roundToInt()
                    root?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handle.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun openHandoff(mode: String, tourId: String, legIndex: Int) {
        context.startActivity(
            TransitHandoffActivity.createIntent(context, mode, tourId, legIndex)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1_000.0) "%.1f km".format(meters / 1_000.0) else "${meters.roundToInt()} m"
}
