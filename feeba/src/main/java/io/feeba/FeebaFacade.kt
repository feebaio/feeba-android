package io.feeba

import io.feeba.data.FeebaConfig
import io.feeba.data.FeebaResponse
import io.feeba.data.LocalStateHolder
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import io.feeba.lifecycle.TriggerValidator
import io.least.core.ServerConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object FeebaFacade {

    // Bookiping referenced directly from internal classes
    lateinit var config: FeebaConfig
    lateinit var localStateHolder: LocalStateHolder
    private lateinit var stateManager: StateManager

    private lateinit var triggerValidator: TriggerValidator

    fun init(
        serverConfig: ServerConfig,
        localStateHolder: LocalStateHolder,
        stateManager: StateManager
    ) {
        Logger.log(LogLevel.DEBUG, "Initialization....")
        this.stateManager = stateManager
        this.localStateHolder = localStateHolder
        triggerValidator = TriggerValidator()
        config = FeebaConfig(serverConfig)

        // We force refresh the Feeba config
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            localStateHolder.forceRefreshFeebaConfig()
        }
    }

    fun updateServerConfig(serverConfig: ServerConfig) {
        Logger.log(LogLevel.DEBUG, "updateServerConfig -> $serverConfig")
        config = FeebaConfig(serverConfig)
        // We force refresh the Feeba config
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            localStateHolder.forceRefreshFeebaConfig()
        }
    }

    fun triggerEvent(eventName: String, value: String? = null) {
        Logger.log(LogLevel.DEBUG, "onEvent -> $eventName, value: $value")
        // check if we have a survey for this event
        val validatorResult = triggerValidator.onEvent(eventName, value, localStateHolder)
        validatorResult?.let {
            localStateHolder.surveyExecutionPlanned(eventName, value.orEmpty(), it.surveyPlan.id)
            stateManager.showEventSurvey(it.surveyPresentation, it.ruleSet, eventName)
        }
    }

    fun pageOpened(pageName: String) {
        Logger.log(LogLevel.DEBUG, "pageOpened -> $pageName")
        // check if we have a survey for this event
        val validationResult = triggerValidator.pageOpened(pageName, localStateHolder)
        validationResult?.let {
            localStateHolder.pageOpened(pageName, "", it.surveyPlan.id)
            stateManager.showPageSurvey(it.surveyPresentation, it.ruleSet, pageName)
        }
    }

    fun pageClosed(pageName: String) {
        Logger.log(LogLevel.DEBUG, "pageClosed -> $pageName")
        localStateHolder.pageClosed(pageName)
        // check if we have a survey for this event
        stateManager.pageClosed(pageName)
    }

    fun onConfigUpdate(callback: ((updatedResponse: FeebaResponse) -> Unit)?) {
        localStateHolder.onConfigUpdate = callback
    }

    object User {
        fun login(userId: String, email: String? = null, phoneNumber: String? = null) {
            Logger.log(LogLevel.DEBUG, "login -> $userId, $email, $phoneNumber")
            localStateHolder.login(userId, email, phoneNumber)
        }

        fun logout() {
            Logger.log(LogLevel.DEBUG, "logout")
            localStateHolder.logout()
        }

        fun addPhoneNumber(phoneNumber: String) {
            Logger.log(LogLevel.DEBUG, "addPhoneNumber -> $phoneNumber")
            localStateHolder.updateUserData(phoneNumber = phoneNumber)
        }

        fun addEmail(email: String) {
            Logger.log(LogLevel.DEBUG, "addEmail -> $email")
            localStateHolder.updateUserData(email = email)
        }

        /**
         * Set the language of the user. This will be used to filter surveys.
         * language: ISO 639-1 code
         */
        fun setLanguage(language: String) {
            Logger.log(LogLevel.DEBUG, "setLanguage -> $language")
            if (language.length != 2) {
                Logger.log(
                    LogLevel.ERROR,
                    "This function expects a ISO 639-1 code. e.g. 'en' for English. Ignoring the call."
                )
                // We won't early terminate the call flow if lang code is not valid
            }
            localStateHolder.updateUserData(language = language)
        }

        fun addTag(tags: Map<String, String>) {
            Logger.log(LogLevel.DEBUG, "addTag -> $tags")
            localStateHolder.addTags(tags = tags)
        }
    }
}
