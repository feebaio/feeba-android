package io.feeba.ui

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.FrameLayout
import android.widget.LinearLayout
import io.feeba.R
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import io.feeba.navigationBarHeight

internal class FloatingViewController(
    private val context: Context,
    private val rootView: ViewGroup,
    private val onKnobClick: (() -> Unit)? = null,
) {
    private var mContentLayout: View? = null
    private var dismissed = false

    fun show() {
        verifyDismissed()
        mContentLayout = createContentView()
        rootView.removeView(mContentLayout)
        rootView.addView(mContentLayout)
    }

    private fun verifyDismissed() {
        if (!dismissed) {
            Logger.log(LogLevel.ERROR, "Tooltip has been dismissed.")
        }
    }

    private fun createContentView(): View {
        val linearLayout = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, ViewUtils.dpToPx(8), context.navigationBarHeight + ViewUtils.dpToPx(8))
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            val layoutPadding = 0
            setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding)
            LayoutInflater.from(context).inflate(R.layout.helper_knob_layout, this, true)

            val ll = this
            setOnTouchListener(object : View.OnTouchListener {
                private var touchStartTime: Long = Long.MAX_VALUE
                private var coordinatesDelta: IntArray? = null
                private var isDragging = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()

                    Logger.log(LogLevel.DEBUG, "onTouch: x=$x, y=$y")
                    Logger.log(LogLevel.DEBUG, "View size: ${v.width}x${v.height}")
                    Logger.log(LogLevel.DEBUG, "View name: ${v.javaClass.simpleName}")
                    return when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // calculate X and Y coordinates of view relative to screen
                            val viewLocation = IntArray(2)
                            v.getLocationOnScreen(viewLocation)
                            Logger.log(LogLevel.DEBUG, "View on Screen: x=${viewLocation[0]} y=${viewLocation[1]}")
                            coordinatesDelta = intArrayOf(x - viewLocation[0], y - viewLocation[1])
                            coordinatesDelta?.let {
                                Logger.log(LogLevel.DEBUG, "coordinatesDelta: x=${it[0]} y=${it[1]}")
                            }
                            // Record the start time of the touch event
                            touchStartTime = System.currentTimeMillis();

                            true // Important to return false so the touch event isn't consumed and is passed to children
                        }

                        MotionEvent.ACTION_MOVE -> {
                            // Calculate new position of the PopupWindow
                            val newX = event.rawX - (coordinatesDelta?.getOrElse(0) { 0 } ?: 0)
                            val newY = event.rawY - (coordinatesDelta?.getOrElse(1) { 0 } ?: 0)

                            // Update the position of the PopupWindow
                            ll.x = newX
                            ll.y = newY
                            isDragging = true
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            // Add any additional logic for when the drag is released if necessary
                            coordinatesDelta = null
                            if (isDragging) {
                                isDragging = false
                                return true // terminate the responder chain
                            }
                            Logger.log(LogLevel.DEBUG, "onTouch: ACTION_UP")
                            if (System.currentTimeMillis() - touchStartTime < ViewConfiguration.getTapTimeout()) {
                                // Consider as a click event
                                onKnobClick?.invoke()
                            }
                            false
                        }

                        else -> false
                    }
                }
            })
        }

        return linearLayout
    }

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        rootView.removeView(mContentLayout)
        mContentLayout?.visibility = View.GONE
        mContentLayout = null
    }
}

fun View.animateFadeIn(
    targetAlpha: Float = 1f,
    setup: ViewPropertyAnimator.() -> ViewPropertyAnimator = { this },
    duration: Long = 1000,
    beforeAnimation: View.() -> Unit = { alpha = 0f },
    afterAnimation: View.() -> Unit = {}
): ViewPropertyAnimator {
    val animator = animate()
        .setup()
        .setDuration(duration)
        .alpha(targetAlpha)
        .withStartAction { beforeAnimation() }
        .withEndAction { this.alpha = targetAlpha; afterAnimation() }
    animator.start()
    return animator
}

fun View.animateFadeOut(
    targetAlpha: Float = 0f,
    setup: ViewPropertyAnimator.() -> ViewPropertyAnimator = { this },
    duration: Long = 1000,
    beforeAnimation: View.() -> Unit = {},
    afterAnimation: View.() -> Unit = {}
): ViewPropertyAnimator {
    val animator = animate()
        .setup()
        .setDuration(duration)
        .alpha(targetAlpha)
        .withStartAction { beforeAnimation() }
        .withEndAction { this.alpha = targetAlpha; afterAnimation() }
    animator.start()
    return animator
}