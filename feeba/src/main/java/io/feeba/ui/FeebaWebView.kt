package io.feeba.ui

import android.content.Context
import android.webkit.WebView

// Custom WebView to lock scrolling
class FeebaWebView(context: Context) : WebView(context) {
    // The method overrides below; overScrollBy, scrollTo, and computeScroll prevent page scrolling
//    public override fun overScrollBy(
//        deltaX: Int, deltaY: Int, scrollX: Int, scrollY: Int,
//        scrollRangeX: Int, scrollRangeY: Int, maxOverScrollX: Int,
//        maxOverScrollY: Int, isTouchEvent: Boolean
//    ): Boolean {
//        return false
//    }
    init {
        overScrollMode = OVER_SCROLL_NEVER
    }
}