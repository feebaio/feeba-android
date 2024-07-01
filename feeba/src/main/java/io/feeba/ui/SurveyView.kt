package io.feeba.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.feeba.FeebaFacade
import io.feeba.Utils
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger


//Create a custom view SurveyView that extends FrameLayout
class SurveyView : FrameLayout {
    private lateinit var webView: FeebaWebView
    private val appHistoryState = FeebaFacade.localStateHolder.readAppHistoryState()

    //Create a constructor that takes in a context
    constructor(context: Context) : super(context) {
        //Call the init function
        init()
    }

    //Create a constructor that takes in a context and an attribute set
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        //Call the init function
        init()
    }

    //Create a constructor that takes in a context, an attribute set and a style
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        //Call the init function
        init()
    }

    //Create a constructor that takes in a context, an attribute set, a style and a default style
    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        //Call the init function
        init()
    }

    //Create a function called init
    private fun init() {
        webView = createWebViewInstanceForManualLoad(context, appHistoryState, onPageLoaded = { webView, loadType ->
            Logger.log(LogLevel.DEBUG, "SurveyView::   onPageLoaded: $loadType")
            if (loadType is PageFrame) {
                removeAllViews()
                addView(webView)
            } else if (loadType is SurveyRendered) {
                // Trigger JS -> Feeba call with document height
//                    webView.loadUrl("javascript:Mobile.resize(document.body.getBoundingClientRect().height)");
            } else if (loadType is PageResized) {
                val updatedWidth = (loadType.w * resources.displayMetrics.density).toInt()
                val updatedHeight = (loadType.h * resources.displayMetrics.density).toInt()
                Logger.d("SurveyView:: w=$updatedWidth, h=${updatedHeight}")
                Utils.runOnMainUIThread {
                    webView.layoutParams = FrameLayout.LayoutParams(updatedHeight, updatedWidth)
                }
            }
        }, onError = {
            // In case of error, remove the view
            removeAllViews()
        })
        isFocusableInTouchMode = true
        requestFocus()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // trigger data push
        webView.evaluateJavascript("window.onInlineViewClosed();", null)
    }

    fun flushResults() {
        // trigger data push
        webView.evaluateJavascript("window.onInlineViewClosed();", null)
    }

    fun loadSurvey(surveyUrl: String) = webView.load(surveyUrl, appHistoryState, IntegrationMode.Inline)

}