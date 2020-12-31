package com.app.meditec.databinding

import android.util.Log
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.meditec.adapters.DirectionStepsAdapter
import com.app.meditec.models.Step

@BindingAdapter("stepList")
fun setStepList(view: RecyclerView, steps: List<Step>?) {
    Log.d("BindingAdapters", "setStepList: ${steps?.size}")
    val layoutManager = view.layoutManager
    if (layoutManager == null)
        view.layoutManager =
                LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
    if (steps != null) {
        view.adapter = DirectionStepsAdapter(view.context, steps)
        val divider = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
        view.addItemDecoration(divider)
    }
}