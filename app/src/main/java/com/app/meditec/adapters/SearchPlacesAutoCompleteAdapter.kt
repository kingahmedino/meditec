package com.app.meditec.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SearchPlacesAutoCompleteAdapter(
        mContext: Context,
        private val mPlacesClient: PlacesClient,
        private val mToken: AutocompleteSessionToken
) : ArrayAdapter<AutocompletePrediction>(mContext, android.R.layout.simple_expandable_list_item_2, android.R.id.text1), Filterable {

    private val style: CharacterStyle = StyleSpan(Typeface.BOLD)
    private lateinit var mPlacePredictions: MutableList<AutocompletePrediction>

    override fun getCount(): Int = mPlacePredictions.size

    override fun getItem(position: Int): AutocompletePrediction? = mPlacePredictions[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)
        val item = getItem(position)

        val textView1 = row.findViewById<TextView>(android.R.id.text1)
        val textView2 = row.findViewById<TextView>(android.R.id.text2)
        if (item != null) {
            textView1.text = item.getPrimaryText(style)
            textView2.text = item.getSecondaryText(style)
        }
        return row
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                var filterData: MutableList<AutocompletePrediction>? = null

                if (constraint != null)
                    filterData = getAutocomplete(constraint)

                results.values = filterData

                if (filterData != null)
                    results.count = filterData.size
                else
                    results.count = 0

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    mPlacePredictions = results.values as MutableList<AutocompletePrediction>
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getFullText(null)
                } else {
                    super.convertResultToString(resultValue)
                }
            }
        }
    }

    private fun getAutocomplete(constraint: CharSequence): MutableList<AutocompletePrediction>? {
        val predictions: MutableList<AutocompletePrediction>?
        val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(mToken)
                .setQuery(constraint.toString())
                .build()

        val task = mPlacesClient.findAutocompletePredictions(request)

        try {
            Tasks.await(task, 60, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

        return try {
            predictions = task.result?.autocompletePredictions
            Log.i("AutoCompleteAdapter", "Query completed. Received ${predictions?.size} predictions.")
            predictions
        } catch (e: RuntimeExecutionException) {
            Log.e("AutoCompleteAdapter", "Error getting autocomplete prediction API call", e)
            null
        }
    }
}