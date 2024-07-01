package io.feeba.survey

import android.app.Activity
import android.view.ViewGroup
import io.feeba.Utils
import io.feeba.data.RuleSet
import io.feeba.data.SurveyPresentation
import io.feeba.data.state.AppHistoryState
import io.feeba.lifecycle.AndroidLifecycleManager
import io.feeba.lifecycle.GenericAppLifecycle
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import io.feeba.ui.FloatingViewController
import io.feeba.ui.SurveyWebViewHolder
import io.feeba.ui.ViewUtils

internal class SurveyViewController(
    private val content: SurveyPresentation,
    private val ruleSet: RuleSet,
    private val appState: AppHistoryState,
    private var viewLifecycleListener: SurveyViewLifecycleListener,
) {
    private var floatingViewController: FloatingViewController? = null

    internal interface SurveyViewLifecycleListener {
        fun onSurveyWasShown()
        fun onSurveyWasDismissed()
    }

    private var marginPxSizeLeft: Int = ViewUtils.dpToPx(24)
    private var marginPxSizeRight: Int = ViewUtils.dpToPx(24)
    private var marginPxSizeTop: Int = ViewUtils.dpToPx(24)
    private var marginPxSizeBottom: Int = ViewUtils.dpToPx(24)

    private var surveyView: SurveyWebViewHolder? = null

    init {
        setMarginsFromContent(content)
    }

    /**
     * For now we only support default margin or no margin.
     * Any non-zero value will be treated as default margin
     * @param content in app message content and style
     */
    private fun setMarginsFromContent(content: SurveyPresentation) {
        marginPxSizeTop = if (content.useHeightMargin) ViewUtils.dpToPx(24) else 0
        marginPxSizeBottom = if (content.useHeightMargin) ViewUtils.dpToPx(24) else 0
        marginPxSizeLeft = if (content.useWidthMargin) ViewUtils.dpToPx(24) else 0
        marginPxSizeRight = if (content.useWidthMargin) ViewUtils.dpToPx(24) else 0
    }

    fun start(lifecycle: GenericAppLifecycle) {
        Logger.log(LogLevel.DEBUG, "SurveyViewController::showSurvey")
        Utils.runOnMainUIThread {
            val currentActivity = (lifecycle as AndroidLifecycleManager).curActivity ?: return@runOnMainUIThread
            if (ruleSet.startWithKnob != null) {
                floatingViewController = FloatingViewController(
                    context = currentActivity.applicationContext,
                    rootView = currentActivity.window.decorView.rootView as ViewGroup,
                ) {
                    floatingViewController?.dismiss()
                    showSurveyUi(currentActivity)
                }

                floatingViewController?.show()
            } else {
                showSurveyUi(currentActivity)
            }
        }
    }

    private fun showSurveyUi(currentActivity: Activity) {
        surveyView?.dismiss()
        surveyView = null
        SurveyWebViewHolder(
            activity = currentActivity,
            rootView = currentActivity.window.decorView.rootView as ViewGroup,
            presentation = content,
            appHistoryState = appState,
            onSurveyClose = {
                surveyView = null
                viewLifecycleListener.onSurveyWasDismissed()
            },
        ).apply {
            surveyView = this
            show()
        }
//            animateSurvey(localDraggableRef)
    }

    fun destroy(callback: Boolean) {
        Logger.log(LogLevel.DEBUG, "SurveyViewController::destroy")
        floatingViewController?.dismiss()
        floatingViewController = null

        surveyView?.dismiss()
        surveyView = null
        if (callback) viewLifecycleListener.onSurveyWasDismissed()
    }
//    private fun animateSurvey(messageView: View) {
//        val messageViewCardView: CardView = messageView.findViewWithTag<CardView>(IN_APP_MESSAGE_CARD_VIEW_TAG)
//        val cardViewAnimCallback = createAnimationListener(messageViewCardView)
//        when (content.displayLocation) {
//            TOP_BANNER -> animateTop(messageViewCardView, webView.height, cardViewAnimCallback)
//            BOTTOM_BANNER -> animateBottom(messageViewCardView, webView.height, cardViewAnimCallback)
//            CENTER_MODAL, FULL_SCREEN -> animateCenter(messageView, backgroundView, cardViewAnimCallback, null)
//        }
//    }
//
//    private fun createAnimationListener(messageViewCardView: CardView): Animation.AnimationListener {
//        return object : Animation.AnimationListener {
//            override fun onAnimationStart(animation: Animation) {}
//            override fun onAnimationEnd(animation: Animation) {
//                // For Android 6 API 23 devices, waits until end of animation to set elevation of CardView class
//                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
//                    messageViewCardView.cardElevation = dpToPx(5f).toFloat()
//                }
//                viewLifecycleListener.onSurveyWasShown()
//            }
//
//            override fun onAnimationRepeat(animation: Animation) {}
//        }
//    }
//
//    private fun animateTop(messageView: View, height: Int, cardViewAnimCallback: Animation.AnimationListener) {
//        // Animate the message view from above the screen downward to the top
//        AnimationUtils.animateViewByTranslation(
//            messageView, (-height - marginPxSizeTop).toFloat(), 0f, IN_APP_BANNER_ANIMATION_DURATION_MS, FeebaBounceInterpolator(0.1, 8.0), cardViewAnimCallback
//        ).start()
//    }
//
//    private fun animateBottom(messageView: View, height: Int, cardViewAnimCallback: Animation.AnimationListener) {
//        // Animate the message view from under the screen upward to the bottom
//        AnimationUtils.animateViewByTranslation(
//            messageView, (height + marginPxSizeBottom).toFloat(), 0f, IN_APP_BANNER_ANIMATION_DURATION_MS, FeebaBounceInterpolator(0.1, 8.0), cardViewAnimCallback
//        ).start()
//    }
//
//    private fun animateCenter(messageView: View, backgroundView: View, cardViewAnimCallback: Animation.AnimationListener, backgroundAnimCallback: Animator.AnimatorListener?) {
//        // Animate the message view by scale since it settles at the center of the screen
//        val messageAnimation: Animation = AnimationUtils.animateViewSmallToLarge(
//            messageView, IN_APP_CENTER_ANIMATION_DURATION_MS, FeebaBounceInterpolator(0.1, 8.0), cardViewAnimCallback
//        )
//
//        // Animate background behind the message so it doesn't just show the dark transparency
//        val backgroundAnimation = animateBackgroundColor(
//            backgroundView, IN_APP_BACKGROUND_ANIMATION_DURATION_MS, ACTIVITY_BACKGROUND_COLOR_EMPTY, ACTIVITY_BACKGROUND_COLOR_FULL, backgroundAnimCallback
//        )
//        messageAnimation.start()
//        backgroundAnimation.start()
//    }

//    private fun animateAndDismissLayout(backgroundView: View) {
//        val animCallback: Animator.AnimatorListener = object : AnimatorListenerAdapter() {
//            override fun onAnimationEnd(animation: Animator) {
//                cleanupViewsAfterDismiss()
//            }
//        }
//
//        // Animate background behind the message so it hides before being removed from the view
//        animateBackgroundColor(
//            backgroundView, IN_APP_BACKGROUND_ANIMATION_DURATION_MS, ACTIVITY_BACKGROUND_COLOR_FULL, ACTIVITY_BACKGROUND_COLOR_EMPTY, animCallback
//        ).start()
//    }
//
//    private fun animateBackgroundColor(backgroundView: View, duration: Int, startColor: Int, endColor: Int, animCallback: Animator.AnimatorListener?): ValueAnimator {
//        return AnimationUtils.animateViewColor(
//            backgroundView, duration, startColor, endColor, animCallback
//        )
//    }
}