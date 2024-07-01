package io.feeba.lifecycle

import android.app.Activity
import android.util.Log
import io.feeba.data.LocalStateHolder
import io.feeba.data.RepeatType
import io.feeba.data.RuleSet
import io.feeba.data.RuleType
import io.feeba.data.ScheduleConfig
import io.feeba.data.StopShowingType
import io.feeba.data.SurveyPlan
import io.feeba.data.SurveyPresentation
import io.feeba.data.SurveyShowStats
import io.feeba.data.TriggerCondition
import io.feeba.data.isEvent
import io.feeba.data.isPageTrigger
import java.util.Date

class TriggerValidator {

    fun onEvent(eventName: String, value: String? = null, localStateHolder: LocalStateHolder): ValidatorResult? {
        Logger.log(LogLevel.DEBUG, "TriggerValidator:: onEvent -> $eventName, value: $value")
        // check if we have a survey for this event
        val config = localStateHolder.readLocalConfig()
        // check if we have a survey for this event
        Logger.log(LogLevel.DEBUG, "TriggerValidator:: surveyPlans -> ${config.surveyPlans}")
        for (surveyPlan in config.surveyPlans) {
            for (ruleSet in surveyPlan.ruleSetList) {
                // if all conditions are met, return the survey
                var allConditionsMet = false
                for (triggerCondition: TriggerCondition in ruleSet.triggers) {
                    if (isEvent(triggerCondition) && triggerCondition.eventName == eventName) {
                        allConditionsMet = true
                    }
                }
                val shouldBeScheduled = areSchedulingConditionsMet(localStateHolder.fetchSurveyShowStats(surveyPlan.id), surveyPlan.distribution?.scheduleConfig)
                Logger.log(LogLevel.DEBUG, "TriggerValidator:: shouldBeScheduled -> $shouldBeScheduled")
                allConditionsMet = allConditionsMet.and(shouldBeScheduled)

                if (allConditionsMet) {
                    return ValidatorResult(surveyPlan.surveyPresentation, 0, ruleSet, surveyPlan)
                }
            }
        }
        return null
    }

    fun pageOpened(pageName: String, localStateHolder: LocalStateHolder): ValidatorResult? {
        Logger.log(LogLevel.DEBUG, "TriggerValidator:: pageOpened -> $pageName")
        // check if we have a survey for this event
        val config = localStateHolder.readLocalConfig()
        // check if we have a survey for this event
        Logger.log(LogLevel.DEBUG, "TriggerValidator:: surveyPlans -> ${config.surveyPlans}")
        for (surveyPlan in config.surveyPlans) {
            for (ruleSet in surveyPlan.ruleSetList) {
                // if all conditions are met, return the survey
                var allConditionsMet = false
                if (isPageTrigger(ruleSet)) {
                    allConditionsMet = true
                }
                if (allConditionsMet) {
                    if (ruleSet.triggers.none { it.eventName == pageName }) {
                        // page name is not in the trigger conditions, we pass the condition block
                        continue
                    }
                    val surveyOpenDelaySec = try {
                        ruleSet.triggers.filter { it.type == RuleType.SESSION_DURATION }
                            .getOrNull(0)?.value?.toLongOrNull() ?: 0
                    } catch (throwable: Throwable) {
                        Logger.log(
                            LogLevel.ERROR,
                            "Failed to parse page timing condition value: $throwable"
                        )
                        0
                    }
                    return ValidatorResult(
                        surveyPlan.surveyPresentation,
                        surveyOpenDelaySec * 1000,
                        ruleSet,
                        surveyPlan
                    )
                }
            }
        }
        return null
    }
}

data class ValidatorResult(
    val surveyPresentation: SurveyPresentation,
    val delay: Long,
    val ruleSet: RuleSet,
    val surveyPlan: SurveyPlan
)

fun validateEvent(triggerCondition: TriggerCondition) {
    when (triggerCondition.conditional) {
        "ex" -> {}
        "eq" -> {}
        "neq" -> {}
        "gt" -> {}
        "gte" -> {}
        "lt" -> {}
        "lte" -> {}
        else -> {
            Logger.log(LogLevel.WARN, "Unknown operator: ${triggerCondition.conditional}")
            null
        }
    }
}

/**
 * Schedule can be null in case it was not set ever. It can be caused by legacy data.
 */
fun areSchedulingConditionsMet(stat: SurveyShowStats, scheduleConfig: ScheduleConfig?): Boolean {
    if (scheduleConfig == null) {
        // Returning false is a safe option in case the data is corrupted or not set. In worst case scenario, the survey will not be shown. Product owner can always update the schedule config.
        return false
    }
    // check if the schedule conditions are met
    // if the show start time is in the past
    val now = Date()
    if (scheduleConfig.showConfig.time.time > now.time) {
        return false
    }
    // if the show end time is in the future
    when (scheduleConfig.stopConfig.selection) {
        StopShowingType.SHOW_FOREVER -> {
            // No need to return false yet. We keep checking other conditions
        }

        StopShowingType.STOP_AFTER_TIME -> {
            if (scheduleConfig.stopConfig.time != null && scheduleConfig.stopConfig.time.time < now.time) {
                return false
            }
        }
    }
    // we check the repeat config
    when (scheduleConfig.repeat.selection) {
        RepeatType.ONCE -> {
            if (stat.executionLogs.isNotEmpty()) {
                return false
            }
        }
        RepeatType.ALWAYS -> {
            // No need to return false yet. We keep checking other conditions
        }
        RepeatType.MANY_TIMES -> {
            // NOT supported yet
        }
    }

    return true
}