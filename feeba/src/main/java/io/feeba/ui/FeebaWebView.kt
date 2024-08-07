package io.feeba.ui

import android.app.Activity
import android.content.Context
import android.view.View.MeasureSpec.EXACTLY
import android.webkit.WebView
import io.feeba.appendQueryParameter
import io.feeba.data.state.AppHistoryState
import io.feeba.lifecycle.Logger
import kotlin.math.max
import kotlin.math.min

// Custom WebView to lock scrolling
class FeebaWebView(context: Context, maxWidthPercent: Int = 100, maxHeightPercent: Int = 100, minWidthPercent: Int = 1, minHeightPercent: Int = 1) : WebView(context) {
    private val maxWidthPx = resources.displayMetrics.widthPixels * maxWidthPercent / 100
    private val maxHeightPx = resources.displayMetrics.heightPixels * maxHeightPercent / 100
    private val minWidthPx = resources.displayMetrics.widthPixels * minWidthPercent / 100
    private val minHeightPx = resources.displayMetrics.heightPixels * minHeightPercent / 100


    init {
        overScrollMode = OVER_SCROLL_NEVER
    }

    @Deprecated("Deprecated in Java")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Adjust width as necessary

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Logger.d("FeebaWebView::onMeasure:measured => $measuredWidth, $measuredHeight")
    }

    @Deprecated("Deprecated. This function is not supported.", ReplaceWith("load(url, headers)"))
    override fun loadUrl(url: String) {
        Logger.w("Do not invoke this call. Deprecated! Call loadUrl(url, headers) instead.")
    }

    fun load(originalUrl: String, appHistoryState: AppHistoryState, integrationMode: IntegrationMode) {
        val queryParamsArray = mutableListOf(
            Pair("im", integrationMode.toString()),
            // Breakpoint
            Pair("bp", readCssBreakPointValue(context as Activity)),
        )
        // if lang code is not found, do not set it to the query params
        appHistoryState.userData?.langCode?.let {
            queryParamsArray.add(Pair("lang", it))
        }

        try {
            val maxWidthWebPixels = maxWidthPx / resources.displayMetrics.density
            val minWidthWebPixels = minWidthPx / resources.displayMetrics.density
            queryParamsArray.add(Pair("mxw", maxWidthWebPixels.toString()))
            queryParamsArray.add(Pair("mnw", minWidthWebPixels.toString()))
        } catch (e: Exception) {
            Logger.e("Error while adding max width to query params: ${e.message}")
        }


        val url = appendQueryParameter(
            originalUrl,
            queryParamsArray
        )
        super.loadUrl(url)
    }
}