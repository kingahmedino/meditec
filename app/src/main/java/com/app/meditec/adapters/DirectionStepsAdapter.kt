package com.app.meditec.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.meditec.databinding.DirectionsItemBinding
import com.app.meditec.models.Step

class DirectionStepsAdapter(private val mContext: Context, private val mSteps: List<Step>)
    : RecyclerView.Adapter<DirectionStepsAdapter.BindingHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        val binding = DirectionsItemBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return BindingHolder(binding.root)
    }

    override fun getItemCount() = mSteps.size

    override fun onBindViewHolder(holder: BindingHolder, position: Int) {
        val step = mSteps[position]
        holder.mBinding?.step = step
        holder.mBinding?.executePendingBindings()
    }

    inner class BindingHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mBinding = DataBindingUtil.bind<DirectionsItemBinding>(itemView)
    }
}