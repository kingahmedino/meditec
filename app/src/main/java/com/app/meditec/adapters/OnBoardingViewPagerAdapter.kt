package com.app.meditec.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.meditec.databinding.OnBoardingScreenLayoutBinding
import com.app.meditec.models.OnBoardingItem

class OnBoardingViewPagerAdapter(private val mContext: Context, onBoardingItems: List<OnBoardingItem>) :
        RecyclerView.Adapter<OnBoardingViewPagerAdapter.BindingHolder>() {

    private val mOnBoardingItems = onBoardingItems
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        val binding: OnBoardingScreenLayoutBinding = OnBoardingScreenLayoutBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return BindingHolder(binding.root)
    }

    override fun onBindViewHolder(holder: BindingHolder, position: Int) {
        val item: OnBoardingItem = mOnBoardingItems[position]
        holder.mBinding?.item = item
        holder.mBinding?.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return mOnBoardingItems.size
    }

    inner class BindingHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mBinding: OnBoardingScreenLayoutBinding? = DataBindingUtil.bind(itemView)
    }

}
