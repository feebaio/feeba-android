package sample.project.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.feeba.data.SurveyPlan
import io.feeba.data.SurveyPresentation
import io.least.demo.R

class EventsAdapter(private val dataSet: List<EventTriggerUiModel>, private val onSelect: (EventTriggerUiModel) -> Unit) : RecyclerView.Adapter<ViewHolder>() {


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_item_event_trigger, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.textViewEvent.text = dataSet[position].event
        viewHolder.textViewExtras.text = dataSet[position].description
        viewHolder.triggerButton.setOnClickListener {
            onSelect(dataSet[position])
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.size
    }
}

/**
 * Provide a reference to the type of views that you are using
 * (custom ViewHolder)
 */
class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textViewEvent: TextView
    val textViewExtras: TextView
    val triggerButton: Button

    init {
        // Define click listener for the ViewHolder's View
        textViewEvent = view.findViewById(R.id.textViewEventName)
        textViewExtras = view.findViewById(R.id.textViewExtra)
        triggerButton = view.findViewById(R.id.buttonTriggerPageActivity)
    }
}

data class EventTriggerUiModel(
    val event: String,
    val description: String,
    val surveyPlan: SurveyPlan
)