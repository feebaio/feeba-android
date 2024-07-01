package sample.project

import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.feeba.Feeba
import io.feeba.data.FeebaResponse
import io.feeba.data.RuleType
import io.least.demo.R
import sample.project.events.EventTriggerUiModel
import sample.project.page.PageTriggerUiModel
import sample.utils.PreferenceWrapper

public fun extractEvents(feebaResponse: FeebaResponse): List<EventTriggerUiModel> {
    return feebaResponse.surveyPlans
        .map { surveyPlan ->
            val eventTrigger = mutableListOf<EventTriggerUiModel>()
            surveyPlan.ruleSetList.forEach {ruleSet ->
                for (trigger in ruleSet.triggers) {
                    if (trigger.type == RuleType.EVENT) {
                        eventTrigger.add(EventTriggerUiModel(trigger.eventName, "", surveyPlan))
                        break
                    }
                }
            }
            eventTrigger
        }
        .flatten()
}

public  fun extractPageTriggers(feebaResponse: FeebaResponse): List<PageTriggerUiModel> {
    val result = mutableListOf<PageTriggerUiModel>()
    // iterate over all survey plans
    for (surveyPlan in feebaResponse.surveyPlans) {
        // iterate over all rule sets
        for (ruleSet in surveyPlan.ruleSetList) {
            var pageTriggerName: String? = null
            var duration = 0
            // iterate over all triggers
            for (trigger in ruleSet.triggers) {
                // if trigger is of type PAGE
                if (trigger.type == RuleType.SCREEN) {
                    // add the trigger to the result list
                    pageTriggerName = trigger.eventName
                } else if (trigger.type == RuleType.SESSION_DURATION) {
                    duration = trigger.value.toInt()
                }
            }
            pageTriggerName?.let {
                result.add(PageTriggerUiModel(it, "Delay: $duration s"))
            }
        }
    }
    return result
}

public  fun prepareLogoutButton(button: View, fragment: Fragment) {
    button.setOnClickListener {
        Feeba.User.logout()
        PreferenceWrapper.jwtToken = ""
        // pop back to the upmost fragment
        fragment.findNavController().navigate(R.id.action_logout)
    }
}