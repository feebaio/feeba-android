package io.feeba.ui

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import androidx.cardview.widget.CardView
import io.feeba.data.Position
import io.feeba.data.SurveyPresentation
import io.feeba.data.state.AppHistoryState
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import io.feeba.navigationBarHeight
import io.feeba.statusBarHeight
import kotlin.math.min

internal class SurveyWebViewHolder(
    private val activity: Activity,
    private val rootView: ViewGroup,
    private val presentation: SurveyPresentation,
    private val appHistoryState: AppHistoryState,
    private val onSurveyClose: () -> Unit
) {
    private val mContentLayout: View
    private var dismissed = false

    init {
        mContentLayout = createContentView()
    }

    fun show() {
        Logger.log(LogLevel.DEBUG, "SurveyWebViewHolder::show")
        verifyDismissed()
        rootView.removeView(mContentLayout)
        rootView.addView(mContentLayout)
    }

    private fun verifyDismissed() {
        if (!dismissed) {
            Logger.log(LogLevel.ERROR, "Tooltip has been dismissed.")
        }
    }

    private fun createContentView(): View {
        val surveyWrapper = createCardView(activity, presentation).apply {
            val cardViewRef = this
            val surveyIntegrationModel: IntegrationMode = if (presentation.displayLocation == Position.FULL_SCREEN) IntegrationMode.FullScreen else IntegrationMode.Modal
            val webView = createWebViewInstance(activity, presentation, appHistoryState, surveyIntegrationModel,
                onPageLoaded = { webView, loadType ->
                    when (loadType) {
                        PageFrame -> {
                            // React is loaded. There is nothing has rendered yet. No need to take actions yet
                        }

                        is PageResized -> {
                            if (presentation.displayLocation == Position.FULL_SCREEN) {
                                // Do nothing. Shortcircut the logic
                                return@createWebViewInstance
                            }

                            // We resize the cardViewRef to respect the restriction set by SurveyPresentation if necessary
                            val maxWidthPercentage = if (presentation.maxWidgetWidthInPercent in 1..100) presentation.maxWidgetWidthInPercent else 90
                            val maxAllowedWidth = (ViewUtils.getWindowWidth(activity) * (maxWidthPercentage / 100f)).toInt()
                            val maxHeightPercentage = if (presentation.maxWidgetHeightInPercent in 1..100) presentation.maxWidgetHeightInPercent else 70
                            val maxAllowedHeight = (ViewUtils.getWindowHeight(activity) * (maxHeightPercentage / 100f)).toInt()

                            //  update height and width of cardViewRef. Use the existing layoutParams
                            Logger.d("PageResized: h=${loadType.h}, maxAllowedHeight=$maxAllowedHeight")
                            Logger.d("PageResized: finalHeight=${if (loadType.h < maxHeightPercentage) loadType.h else maxAllowedHeight}")
//                            setLayoutParamsForCardView(presentation, cardViewRef, maxAllowedWidth, maxAllowedHeight)
                        }

                        SurveyRendered -> {
                            removeAllViews()
                            addView(webView)
                        }
                    }
                },
                onError = {
                    dismiss()
                },
                onOutsideTouch = {
                    dismiss()
                })

            webView.setOnKeyListener { v, keyCode, event ->
                Logger.log(LogLevel.DEBUG, "onKeyListener: keyCode=$keyCode, event=$event")
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dismiss()
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        }

        // This is a frame that covers the entire screen and is used to dismiss the card view when clicked outside of it
        return FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.BOTTOM
            }
            Logger.log(LogLevel.DEBUG, "SurveyWebViewHolder::statusBarHeight: ${activity.statusBarHeight}, navigationBarHeight: ${activity.navigationBarHeight}")
            setPadding(0, activity.statusBarHeight, 0, activity.navigationBarHeight)

            addView(surveyWrapper)
            var viewLocation: IntArray? = null
            setOnTouchListener { v, event ->
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()

                Logger.log(LogLevel.DEBUG, "onTouch: x=$x, y=$y")
                Logger.log(LogLevel.DEBUG, "View size: ${v.width}x${v.height}")
                Logger.log(LogLevel.DEBUG, "View name: ${v.javaClass.simpleName}")
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // calculate X and Y coordinates of view relative to screen
                        viewLocation = IntArray(2)
                        v.getLocationOnScreen(viewLocation)
                        viewLocation?.let {
                            Logger.log(LogLevel.DEBUG, "View on Screen: x=${it[0]} y=${it[1]}")
                        }
                        true // Important to return false so the touch event isn't consumed and is passed to children
                    }

                    MotionEvent.ACTION_UP -> {
                        // Add any additional logic for when the drag is released if necessary
                        Logger.log(LogLevel.DEBUG, "onTouch: ACTION_UP")
                        viewLocation?.let {
                            if (!isPointInsideView(it[0], it[1], surveyWrapper)) {
                                // Consider as a click event
                                dismiss()
                            }
                        }
                        false
                    }

                    else -> false
                }
            }
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { v, keyCode, event ->
                Logger.log(LogLevel.DEBUG, "onKeyListener: keyCode=$keyCode, event=$event")
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dismiss()
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        }
    }

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        rootView.removeView(mContentLayout)
        mContentLayout.visibility = View.GONE
        onSurveyClose()
    }
}


/**
 * To show drop shadow on WebView
 * Layout container for WebView is needed
 */
private fun createCardView(activity: Activity, content: SurveyPresentation): CardView {
    val cardView = CardView(activity.applicationContext).apply {
        setLayoutParamsForCardView(content, this)
    }
    cardView.cardElevation = ViewUtils.dpToPx(5).toFloat()
    cardView.radius = ViewUtils.dpToPx(8).toFloat()
    cardView.clipChildren = false
    cardView.clipToPadding = false
    cardView.preventCornerOverlap = false
    cardView.setCardBackgroundColor(Color.WHITE)
    return cardView
}

private fun setLayoutParamsForCardView(content: SurveyPresentation, cardView: CardView, width: Int = FrameLayout.LayoutParams.WRAP_CONTENT, height: Int = FrameLayout.LayoutParams.WRAP_CONTENT) {
    val params = FrameLayout.LayoutParams(width, height)
    when (content.displayLocation) {
        Position.TOP_BANNER -> {
            params.gravity = Gravity.CENTER or Gravity.TOP
        }

        Position.BOTTOM_BANNER -> {
            params.gravity = Gravity.CENTER or Gravity.BOTTOM
        }

        Position.FULL_SCREEN -> {
            params.gravity = Gravity.CENTER
            params.height = FrameLayout.LayoutParams.MATCH_PARENT
            params.width = FrameLayout.LayoutParams.MATCH_PARENT
        }

        Position.CENTER_MODAL -> {
            params.gravity = Gravity.CENTER
        }
    }
    cardView.layoutParams = params
}

private fun adjustCardViewSize(activity: Activity, cardView: View, presentation: SurveyPresentation, width: Int, height: Int) {
    Logger.d("SurveyWebViewHolder::Activity height -> ${ViewUtils.getWindowHeight(activity)}, width -> ${ViewUtils.getWindowWidth(activity)}")
    Logger.d("SurveyWebViewHolder::cardViewInstance::height: $height, width: $width")

    if (presentation.displayLocation != Position.FULL_SCREEN) {
        // Set the actual size of the card view based on the content
        val heightPercentage: Int = if (presentation.maxWidgetHeightInPercent in 1..100) presentation.maxWidgetHeightInPercent else 70
        val widthPercentage: Int = if (presentation.maxWidgetWidthInPercent in 1..100) presentation.maxWidgetWidthInPercent else 90
        val maxAllowedHeight = (ViewUtils.getWindowHeight(activity) * (heightPercentage / 100f)).toInt()
        val maxAllowedWidth = (ViewUtils.getWindowWidth(activity) * (widthPercentage / 100f)).toInt()
        Logger.d("SurveyWebViewHolder:: maxAllowedHeight: $maxAllowedHeight, maxAllowedWidth: $maxAllowedWidth")
        cardView.layoutParams = cardView.layoutParams.also {
            it.height = min(maxAllowedHeight, height)
            it.width = min(maxAllowedWidth, width)
        }
    }
}