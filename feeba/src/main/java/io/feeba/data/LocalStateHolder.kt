package io.feeba.data

import io.feeba.ServiceLocator
import io.feeba.data.state.AppHistoryState
import io.feeba.data.state.Defaults
import io.feeba.data.state.StateStorageInterface
import io.feeba.data.state.SurveyExecutionLogs
import io.feeba.data.state.UserData
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger

const val TAG = "LocalStateHolder::"

class LocalStateHolder(
    private val stateStorage: StateStorageInterface,
    private val restClient: RestClient
) {
    @Volatile
    var lastKnownFeebaConfig: FeebaResponse? = null
        set(value) {
            field = value
            value?.let { onConfigUpdate?.invoke(it) }
        }

    // callback to notify the listeners about the updated config
    var onConfigUpdate: ((updatedResponse: FeebaResponse) -> Unit)? = null
        set(value) {
            field = value
            // Notify the listener about the last known config
            lastKnownFeebaConfig?.let { value?.invoke(it) }
        }
    private val lastKnownAppHistoryState: AppHistoryState? = null

    private val eventCountMap = mutableMapOf<String, Int>()
    private val jsonInstance = ServiceLocator.jsonInstance

    suspend fun forceRefreshFeebaConfig() {
        Logger.log(LogLevel.DEBUG, "${TAG}forceRefreshFeebaConfig")
        restClient.getSurveyPlans(readAppHistoryState())?.let {
            Logger.log(LogLevel.DEBUG, "${TAG} Survey plans fetched: $it")
            setFeebaConfig(it)
        }
    }

    fun setFeebaConfig(response: String) {
        Logger.log(LogLevel.DEBUG, "${TAG} Storing response: $response")
        // Update local reference
        try {
            jsonInstance.decodeFromString<FeebaResponse>(response).also {
                this.lastKnownFeebaConfig = it
                stateStorage.feebaResponse = it
            }
        } catch (t: Throwable) {
            Logger.log(LogLevel.ERROR, "${TAG} Failed to parse response. Error: $t")
        }
    }


    fun readLocalConfig(): FeebaResponse {
        return lastKnownFeebaConfig ?: run {
            stateStorage.feebaResponse
        }
    }


    fun readAppHistoryState(): AppHistoryState {
        return lastKnownAppHistoryState ?: run {
            try {
                stateStorage.state
            } catch (t: Throwable) {
                Logger.e("${TAG} Failed to read local config. Falling back to default state. Error: $t")
                return Defaults.appHistoryState
            }
        }
    }

    fun login(userId: String, email: String?, phoneNumber: String?) {
        // Try to use the existing state. There can be cases where the app start the Feeba and sets languages at the start and login may or may not happen later on.
        readAppHistoryState().apply {
            this.userData = UserData(
                userId = userId,
                email = email,
                phoneNumber = phoneNumber,
                langCode = this.userData?.langCode,
                tags = this.userData?.tags ?: mutableMapOf()
            )
            stateStorage.state = this
        }
    }

    fun logout() {
        readAppHistoryState()
        stateStorage.eraseEventAndPageLogs()
    }

    fun surveyExecutionPlanned(eventName: String, payload: String, triggeredSurveyId: String) {
        eventCountMap[eventName] = (eventCountMap[eventName] ?: 0) + 1
        stateStorage.addEventRecord(eventName, payload, triggeredSurveyId)
    }

    fun pageOpened(pageName: String, value: String, triggeredSurveyId: String) {
        stateStorage.addPageOpenRecord(pageName, value, triggeredSurveyId)
    }

    fun pageClosed(pageName: String) {
//        TODO("Not yet implemented")
    }

    fun updateUserData(
        phoneNumber: String? = null,
        email: String? = null,
        language: String? = null,
        tags: Map<String, String>? = null,
    ) {
        readAppHistoryState().apply {

            val updatedUserData: UserData = userData?.copy(
                phoneNumber = phoneNumber ?: userData?.phoneNumber,
                email = email ?: userData?.email,
                langCode = language ?: userData?.langCode, // this line either uses the new language or the existing one
                tags = (userData?.tags ?: mutableMapOf()).apply { putAll(tags ?: mutableMapOf()) }
            ) ?: UserData(
                userId = "empty",
                phoneNumber = phoneNumber,
                email = email,
                langCode = language,
                tags = tags?.toMutableMap() ?: mutableMapOf(),
            )

            Logger.d("${TAG}updateUserData: $updatedUserData")
            this.userData = updatedUserData
            stateStorage.state = this
        }
    }

    fun addTags(tags: Map<String, String>) {
        readAppHistoryState().apply {
            this.userData?.tags?.putAll(tags)
            // Ensure that new additions are persisted
            stateStorage.state = this
        }
    }

    fun fetchSurveyShowStats(surveyId: String): SurveyShowStats {
        val surveyShowRecords = stateStorage.readEventLogs(surveyId)
        return SurveyShowStats(
            surveyId = surveyId,
            executionLogs = surveyShowRecords
        )
    }
}

data class SurveyShowStats(
    val surveyId: String,
    val executionLogs: List<SurveyExecutionLogs>
)