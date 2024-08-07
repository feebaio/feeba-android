package io.feeba.ui

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.net.http.SslError
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.webkit.ConsoleMessage
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import io.feeba.Utils
import io.feeba.appendQueryParameter
import io.feeba.data.Position
import io.feeba.data.SurveyPresentation
import io.feeba.data.state.AppHistoryState
import io.feeba.getSanitizedHeightPercent
import io.feeba.getSanitizedWidthPercent
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import io.feeba.survey.CallToAction
import io.feeba.survey.JsInterface
import java.lang.ref.WeakReference

internal object ViewUtils {
    private val MARGIN_ERROR_PX_SIZE = dpToPx(24)

    /**
     * Check if the keyboard is currently being shown.
     * Does not work for cases when keyboard is full screen.
     */
    fun isKeyboardUp(activityWeakReference: WeakReference<Activity?>): Boolean {
        val metrics = DisplayMetrics()
        val visibleBounds = Rect()
        var view: View? = null
        var isOpen = false
        if (activityWeakReference.get() != null) {
            val window = activityWeakReference.get()!!.window
            view = window.decorView
            view.getWindowVisibleDisplayFrame(visibleBounds)
            window.windowManager.defaultDisplay.getMetrics(metrics)
        }
        if (view != null) {
            val heightDiff = metrics.heightPixels - visibleBounds.bottom
            isOpen = heightDiff > MARGIN_ERROR_PX_SIZE
        }
        return isOpen
    }

    // Ensures the root decor view is ready by checking the following;
    //   1. Is fully attach to the root window and insets are available
    //   2. Ensure if any Activities are changed while waiting we use the updated one
    fun decorViewReady(activity: Activity, runnable: Runnable) {
        val listenerKey = "decorViewReady:$runnable"
        activity.window.decorView.post {
            if (isActivityFullyReady(activity)) runnable.run()
        }
    }

    private fun getWindowVisibleDisplayFrame(activity: Activity): Rect {
        val rect = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
        return rect
    }

    fun getCutoutAndStatusBarInsets(activity: Activity): IntArray {
        val frame = getWindowVisibleDisplayFrame(activity)
        val contentView = activity.window.findViewById<View>(Window.ID_ANDROID_CONTENT)
        var rightInset = 0f
        var leftInset = 0f
        val topInset = (frame.top - contentView.top) / Resources.getSystem().displayMetrics.density
        val bottomInset =
            (contentView.bottom - frame.bottom) / Resources.getSystem().displayMetrics.density
        // API 29 is the only version where the IAM bleeds under cutouts in immersize mode
        // All other versions will not need left and right insets.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val cutout = activity.windowManager.defaultDisplay.cutout
            if (cutout != null) {
                rightInset = cutout.safeInsetRight / Resources.getSystem().displayMetrics.density
                leftInset = cutout.safeInsetLeft / Resources.getSystem().displayMetrics.density
            }
        }
        return intArrayOf(
            Math.round(topInset),
            Math.round(bottomInset),
            Math.round(rightInset),
            Math.round(leftInset)
        )
    }

    fun getFullbleedWindowWidth(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = activity.window.decorView
            decorView.width
        } else {
            getWindowWidth(activity)
        }
    }

    fun getWindowWidth(activity: Activity): Int {
        return getWindowVisibleDisplayFrame(activity).width()
    }

    // Due to differences in accounting for keyboard, navigation bar, and status bar between
    //   Android versions have different implementation here
    fun getWindowHeight(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getWindowHeightAPI23Plus(activity) else getWindowHeightLollipop(
            activity
        )
    }

    // Requirement: Ensure DecorView is ready by using ViewUtils.decorViewReady
    @TargetApi(Build.VERSION_CODES.M)
    private fun getWindowHeightAPI23Plus(activity: Activity): Int {
        val decorView = activity.window.decorView
        // Use use stable heights as SystemWindowInset subtracts the keyboard
        val windowInsets = decorView.rootWindowInsets ?: return decorView.height
        return decorView.height -
                windowInsets.stableInsetBottom -
                windowInsets.stableInsetTop
    }

    private fun getWindowHeightLollipop(activity: Activity): Int {
        // getDisplaySizeY - works correctly expect for landscape due to a bug.
        return if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) getWindowVisibleDisplayFrame(
            activity
        ).height() else getDisplaySizeY(
            activity
        )
        //  getWindowVisibleDisplayFrame - Doesn't work for portrait as it subtracts the keyboard height.
    }

    private fun getDisplaySizeY(activity: Activity): Int {
        val point = Point()
        activity.windowManager.defaultDisplay.getSize(point)
        return point.y
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    // Ensures the Activity is fully ready by;
    //   1. Ensure it is attached to a top-level Window by checking if it has an IBinder
    //   2. If Android M or higher ensure WindowInsets exists on the root window also
    fun isActivityFullyReady(activity: Activity): Boolean {
        val hasToken = activity.window.decorView.applicationWindowToken != null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return hasToken
        val decorView = activity.window.decorView
        val insetsAttached = decorView.rootWindowInsets != null
        return hasToken && insetsAttached
    }

    fun removeOnGlobalLayoutListener(
        view: View,
        listener: ViewTreeObserver.OnGlobalLayoutListener?
    ) {
        view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }
}

internal fun Fragment.showKeyboard(view: View) {
    val inputMethodManager =
        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_FORCED)
}

internal fun Fragment.closeKeyboard() {
    val inputMethodManager =
        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.let { im ->
        activity?.currentFocus?.windowToken?.let { binder ->
            im.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_IMPLICIT_ONLY, null)
        }
    }
}

fun isPointInsideView(xToCheck: Int, yToCheck: Int, view: View): Boolean {
    return xToCheck >= view.x && xToCheck <= view.x + view.width
            && yToCheck >= view.y && yToCheck <= view.y + view.height
}

/**
 * This function is used when we already have a URL to load. So we create a WebView and immediately load the URL
 */
fun createWebViewInstance(
    context: Context, presentation: SurveyPresentation, appHistoryState: AppHistoryState,
    integrationMode: IntegrationMode,
    onPageLoaded: (WebView, LoadType) -> Unit,
    onError: () -> Unit, onOutsideTouch: (() -> Unit)?,
): FeebaWebView {
    var wrapType = if (presentation.displayLocation == Position.FULL_SCREEN) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT
    return createWebViewInstanceForManualLoad(
        context,
        appHistoryState,
        onError,
        onPageLoaded,
        onOutsideTouch,
        getSanitizedWidthPercent(presentation.maxWidgetWidthInPercent), getSanitizedHeightPercent(presentation.maxWidgetHeightInPercent),
        wrapType, wrapType
    ).apply {
        // Pass the URL as it is, FeebaWebView will append query params when needed.
        load(presentation.surveyWebAppUrl, appHistoryState, integrationMode)
    }
}

/**
 * This function is used when you don't have a URL to load yet, but load the URL later to the existing FeebaInlineView
 */
fun createWebViewInstanceForManualLoad(
    context: Context, appHistoryState: AppHistoryState,
    onError: () -> Unit,
    onPageLoaded: (WebView, LoadType) -> Unit,
    onOutsideTouch: (() -> Unit)? = null,
    maxWidth: Int = 100,
    maxHeight: Int = 100,
    width: Int = FrameLayout.LayoutParams.WRAP_CONTENT, height: Int = FrameLayout.LayoutParams.WRAP_CONTENT
): FeebaWebView {
    return FeebaWebView(context, maxWidth, maxHeight).apply {
        WebView.setWebContentsDebuggingEnabled(true)

        layoutParams = FrameLayout.LayoutParams(width, height)

        isNestedScrollingEnabled = true
        settings.apply {
            javaScriptEnabled = true
            useWideViewPort = false
            allowFileAccess = true
            domStorageEnabled = true

            setSupportZoom(false)
            setSupportZoom(false)
            setGeolocationEnabled(true)
            setLightTouchEnabled(true)
            // Below is trying to fetch a JS bundle that is outdated. Requires deeper investigation
        }
        addJavascriptInterface(
            JsInterface(
                appHistoryState,
                onSurveyFullyRendered = {
                    Utils.runOnMainUIThread { onPageLoaded(this, SurveyRendered) }
                },
                onSurveyEndCallback = {
                    when (it) {
                        CallToAction.CLOSE_SURVEY -> {
                            Logger.log(LogLevel.DEBUG, "FeebaWebView::JsInterface::CallToAction.CLOSE_SURVEY")
                            onOutsideTouch?.invoke()
                        }
                    }
                },
                onResize = { w, h ->
                    val wPx = (w * context.resources.displayMetrics.density).toInt()
                    val hPx = (h * context.resources.displayMetrics.density).toInt()
                    Logger.d("FeebaWebView::JsInterface::onResize, width=${wPx}, height=${hPx}, density=${context.resources.displayMetrics.density}")
                    Utils.runOnMainUIThread { onPageLoaded(this, PageResized(wPx, hPx)) }
                }),
            "Mobile"
        )
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Logger.log(LogLevel.DEBUG, "WebViewClient::onPageStarted, url: $url")
            }

            override fun onPageFinished(view: WebView, url: String?) {
                Logger.log(LogLevel.DEBUG, "WebViewClient::onPageFinished, url: $url")
                onPageLoaded(view, PageFrame)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError
            ) {
                Logger.log(LogLevel.ERROR, "WebViewClient::onReceivedError, error: $error")
                // Log WebView errors here
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Log error details on API level 23 and above
                    Logger.log(
                        LogLevel.ERROR,
                        "WebViewClient::onReceivedError, description: ${error.description}"
                    )
                } else {
                    // Log error details on API level below 23
                    Logger.log(
                        LogLevel.ERROR,
                        "WebViewClient::onReceivedError, description: ${error}"
                    )
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)

                Logger.log(
                    LogLevel.ERROR,
                    "WebViewClient::onReceivedHttpError, request.urlAd: ${request.url}"
                )
                Logger.log(
                    LogLevel.ERROR,
                    "WebViewClient::onReceivedHttpError, statusCode: ${errorResponse.statusCode}"
                )
//                Logger.log(LogLevel.ERROR, "WebViewClient::onReceivedHttpError, data: ${errorResponse.data.use { it.reader().readText() } }}")
                onError()
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                super.onReceivedSslError(view, handler, error)
                Logger.log(LogLevel.ERROR, "WebViewClient::onReceivedSslError, error: $error")
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Logger.log(
                    LogLevel.DEBUG,
                    "WebChromeClient::onConsoleMessage, message: ${consoleMessage.message()}"
                )
//                Logger.log(LogLevel.DEBUG, "WebChromeClient::onConsoleMessage full: $consoleMessage")

                return true
            }

        }
    }
}

/**
 * xs, extra-small: 0px
 * sm, small: 600px
 * md, medium: 960px
 * lg, large: 1280px
 * xl, extra-large: 1920px
 */
fun readCssBreakPointValue(activity: Activity) = ViewUtils.getWindowWidth(activity).let {
    when {
        it < 600 -> "xs"
        it < 960 -> "sm"
        it < 1280 -> "md"
        it < 1920 -> "lg"
        else -> "xl"
    }
}

sealed interface LoadType
data object PageFrame : LoadType;
data object SurveyRendered : LoadType;
class PageResized(val w: Int, val h: Int) : LoadType;

/**
 * This value is translated into "im" query param in the URL
 */
enum class IntegrationMode {
    FullScreen, Inline, Modal;

    override fun toString() = when (this) {
        FullScreen -> "f"
        Inline -> "i"
        Modal -> "m"
        else -> {
            // Fallback value is Modal for Mobile SDKs
            Logger.w("IntegrationMode::toString, unknown value: $this")
            "m"
        }
    }
}
