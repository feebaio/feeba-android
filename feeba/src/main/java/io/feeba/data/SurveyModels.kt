package io.feeba.data

import io.least.core.ServerConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class FeebaResponse(
    val surveyPlans: List<SurveyPlan>,
    val sdkConfig: SdkConfig? = null,
    val inlineSurveys: List<InlineSurvey>? = null
)

@Serializable
data class SurveyPlan(
    val id: String,
    val surveyPresentation: SurveyPresentation,
    val ruleSetList: List<RuleSet>,
    val distribution: DistributionModel? = null
)

@Serializable
enum class RuleType () {
    @SerialName("event") EVENT,
    @SerialName("session_duration") SESSION_DURATION,
    @SerialName("since_last") SINCE_LAST,
    @SerialName("screen") SCREEN,
    @SerialName("app_open") APP_OPEN
}

@Serializable
data class RuleSet (
    val triggers: List<TriggerCondition>,
    val startWithKnob: HelperKnob?
)

@Serializable
data class TriggerCondition(
    val type: RuleType,
    val eventName: String,
    val conditional: String,
    val value: String
)

@Serializable
data class SdkConfig    (
    val refreshIntervalSec: Int,
    val baseServerUrl: String? = null,
)

data class FeebaConfig(
    val serviceConfig : ServerConfig,
)

@Serializable
data class SurveyPresentation(
    val surveyWebAppUrl: String,
    val useHeightMargin: Boolean,
    val useWidthMargin: Boolean,
    val isFullBleed: Boolean,
    // The following properties are populated from Javascript events
    val displayLocation: Position = Position.TOP_BANNER,
    val displayDuration: Double,
    val maxWidgetHeightInPercent: Int = 70, // between 0 to 100
    val maxWidgetWidthInPercent: Int = 90, // between 0 to 100
): java.io.Serializable

@Serializable
data class HelperKnob(
    val hintText: String? = null,
)
@Serializable
enum class Position {
    @SerialName("top_banner") TOP_BANNER,
    @SerialName("bottom_banner") BOTTOM_BANNER,
    @SerialName("center_modal") CENTER_MODAL,
    @SerialName("full_screen") FULL_SCREEN;
}

fun isBanner(position: Position): Boolean =
    when (position) {
        Position.TOP_BANNER, Position.BOTTOM_BANNER -> true
        else -> false
    }

@Serializable
data class InlineSurvey(
    val id: String,
    val surveyName: String,
    val webPageUrl: String,
)

// Distribution plan
@Serializable
data class DistributionModel(
    val scheduleConfig: ScheduleConfig,
)

@Serializable
data class ScheduleConfig(
    val showConfig: ShowConfig,
    val stopConfig: StopConfig,
    val repeat: RepeatConfig,
)

@Serializable
data class ShowConfig(
    @Serializable(with = DateSerializer::class)
    val time: Date,
)

@Serializable
enum class StopShowingType {
    @SerialName("show_forever") SHOW_FOREVER,
    @SerialName("show_until") STOP_AFTER_TIME, // stop after a certain time
}
@Serializable
data class StopConfig(
    val selection: StopShowingType,
    @Serializable(with = DateSerializer::class)
    val time: Date? = null,
)

@Serializable
enum class RepeatType {
    @SerialName("once") ONCE, // show only once
    @SerialName("always") ALWAYS, // show every time when the condition is met
    @SerialName("many_times") MANY_TIMES, // show many times
}
@Serializable
data class RepeatConfig(
    val selection: RepeatType,
)