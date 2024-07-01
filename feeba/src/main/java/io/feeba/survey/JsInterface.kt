package io.feeba.survey

import android.webkit.JavascriptInterface
import io.feeba.ServiceLocator
import io.feeba.data.state.AppHistoryState
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import kotlinx.serialization.encodeToString

enum class CallToAction(val value: String) {
    CLOSE_SURVEY("closeSurvey");

    companion object {
        fun safeValueOf(value: String) = values().find { it.value == value } ?: CLOSE_SURVEY
    }
}

class JsInterface(
    private val appHistoryState: AppHistoryState,
    private val onSurveyFullyRendered: () -> Unit,
    private val onSurveyEndCallback: (cta: CallToAction) -> Unit,
    private val onResize: (width: Int, height: Int) -> Unit
) {
    private val jsonInstance = ServiceLocator.jsonInstance

    init {
        Logger.log(LogLevel.DEBUG, "JsInterface::init, appHistoryState=$appHistoryState")
    }

    /** Show a toast from the web page  */
    @JavascriptInterface
    fun endOfSurvey(callToAction: String) {
        val cta: CallToAction = CallToAction.safeValueOf(callToAction)
        onSurveyEndCallback(cta)
    }

    @JavascriptInterface
    fun getCurrentState(): String {
        Logger.log(LogLevel.DEBUG, "JsInterface::getCurrentState, appHistoryState=$appHistoryState")
        return jsonInstance.encodeToString(appHistoryState)
    }

    @JavascriptInterface
    fun surveyFullyRendered() {
        Logger.log(LogLevel.DEBUG, "JsInterface::onSurveyFullyRendered")
        onSurveyFullyRendered()
    }

    /**
     * Returns unparsed server response back to the webview.
     */
    @JavascriptInterface
    suspend fun getPrefetchedSurveyV1(
        projectName: String,
        surveyId: String,
        lang: String?
    ): String? {
        Logger.log(LogLevel.DEBUG, "JsInterface::onSurveyFullyRendered")
        onSurveyFullyRendered()
        return null
    }

    @JavascriptInterface
    fun resize(width: Int, height: Int) {
        Logger.log(LogLevel.DEBUG, "JsInterface::resize, width=$width, height=$height")
        onResize(width, height)
    }
}