package com.altair.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val pages: List<OnboardingPageData>,
    private val onTermsCheckedChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val illustrationImage: ImageView = itemView.findViewById(R.id.imgIllustration)
        val titleText: TextView = itemView.findViewById(R.id.txtTitle)
        val bodyText: TextView = itemView.findViewById(R.id.txtBody)
        val termsCheckbox: CheckBox? = itemView.findViewById(R.id.chkTerms)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.onboarding_page, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = pages[position]

        holder.illustrationImage.setImageResource(page.imageRes)
        holder.titleText.text = page.title
        holder.bodyText.text = page.body

        if (holder.termsCheckbox != null) {
            if (page.showCheckbox) {
                holder.termsCheckbox.visibility = View.VISIBLE

                holder.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    onTermsCheckedChanged(isChecked)
                }
            } else {
                holder.termsCheckbox.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = pages.size
}