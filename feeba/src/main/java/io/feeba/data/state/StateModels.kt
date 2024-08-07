package io.feeba.data.state

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val userId: String,
    val email: String?,
    val phoneNumber: String?,
    val tags: MutableMap<String, String>,
    val langCode: String?, // ISO 639-1
)

@Serializable
data class AppHistoryState(
    var numberOfLaunches: Int,
    var totalSessionDurationSec: Long,

    var lastTimeAppOpened: Long,
    var lastTimeSurveyTriggered: Map<String, Long>,

    var userData: UserData?,
)

object Defaults {
    val appHistoryState = AppHistoryState(
        numberOfLaunches = 0,
        totalSessionDurationSec = 0,
        lastTimeAppOpened = 0,
        lastTimeSurveyTriggered = mapOf(),
        userData = UserData(
            userId = "",
            email = "",
            phoneNumber = "",
            tags = mutableMapOf(),
            langCode = null, // Intentionally left NULL, so if not set for whatever reason, the default language dictated by backend will be selected
        ),
    )
}
