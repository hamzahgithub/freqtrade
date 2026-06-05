package com.oilsite.analyzer.ui.analysis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oilsite.analyzer.R
import com.oilsite.analyzer.data.model.ReadingStatus
import com.oilsite.analyzer.data.model.ValidationResult
import com.oilsite.analyzer.databinding.ItemReadingBinding
import java.util.Locale

class ReadingAdapter : ListAdapter<ValidationResult, ReadingAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemReadingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: ValidationResult) {
            binding.tvParameter.text = result.parameterAr
            binding.tvParameterEn.text = result.parameter
            binding.tvValue.text = String.format(Locale.US, "%.2f %s", result.value, result.unit)
            binding.tvStatusLabel.text = result.status.labelAr()
            binding.tvMessage.text = result.messageAr
            binding.tvRecommendation.text = result.recommendationAr ?: ""

            val (bgColor, textColor, iconRes) = when (result.status) {
                ReadingStatus.NORMAL -> Triple(
                    R.color.status_normal_bg,
                    R.color.status_normal_text,
                    R.drawable.ic_check_circle
                )
                ReadingStatus.WARNING -> Triple(
                    R.color.status_warning_bg,
                    R.color.status_warning_text,
                    R.drawable.ic_warning
                )
                ReadingStatus.CRITICAL -> Triple(
                    R.color.status_critical_bg,
                    R.color.status_critical_text,
                    R.drawable.ic_error
                )
                ReadingStatus.UNDERHEAT -> Triple(
                    R.color.status_underheat_bg,
                    R.color.status_underheat_text,
                    R.drawable.ic_thermostat_low
                )
                ReadingStatus.UNKNOWN -> Triple(
                    R.color.status_unknown_bg,
                    R.color.status_unknown_text,
                    R.drawable.ic_help
                )
            }

            binding.cardRoot.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, bgColor)
            )
            binding.tvStatusLabel.setTextColor(
                ContextCompat.getColor(binding.root.context, textColor)
            )
            binding.ivStatusIcon.setImageResource(iconRes)
            binding.ivStatusIcon.setColorFilter(
                ContextCompat.getColor(binding.root.context, textColor)
            )

            binding.tvRecommendation.visibility =
                if (result.recommendationAr.isNullOrBlank()) android.view.View.GONE
                else android.view.View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemReadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ValidationResult>() {
            override fun areItemsTheSame(a: ValidationResult, b: ValidationResult) =
                a.parameter == b.parameter
            override fun areContentsTheSame(a: ValidationResult, b: ValidationResult) = a == b
        }
    }
}
