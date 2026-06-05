package com.oilsite.analyzer.ui.analysis

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.oilsite.analyzer.R
import com.oilsite.analyzer.data.model.ReadingStatus
import com.oilsite.analyzer.databinding.FragmentAnalysisBinding

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private val args: AnalysisFragmentArgs by navArgs()
    private val viewModel: AnalysisViewModel by viewModels()
    private val adapter = ReadingAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvReadings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReadings.adapter = adapter

        Glide.with(this)
            .load(Uri.parse(args.imageUri))
            .centerCrop()
            .into(binding.ivCapturedImage)

        observeState()

        if (!viewModel.hasApiKey) {
            Snackbar.make(binding.root, R.string.no_api_key_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.settings) {
                    findNavController().navigate(R.id.action_analysis_to_settings)
                }.show()
            return
        }

        viewModel.analyze(args.imageUri)
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AnalysisState.Idle -> showIdle()
                is AnalysisState.Loading -> showLoading()
                is AnalysisState.Success -> showResults(state)
                is AnalysisState.Error -> showError(state.message)
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.groupResults.visibility = View.GONE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.analyzing_image)
        binding.groupResults.visibility = View.GONE
    }

    private fun showResults(state: AnalysisState.Success) {
        val report = state.report
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.groupResults.visibility = View.VISIBLE

        // Equipment info
        binding.tvEquipmentType.text = translateEquipmentType(report.equipmentType)
        binding.tvEquipmentDetails.text = report.equipmentDetails
        binding.tvNotes.text = report.notes.ifBlank { "—" }

        // Summary badges
        binding.tvNormalCount.text = report.normalCount.toString()
        binding.tvWarningCount.text = report.warningCount.toString()
        binding.tvCriticalCount.text = (report.criticalCount + report.underHeatCount).toString()

        // Overall status banner
        val (bannerColor, bannerText) = when (report.overallStatus) {
            ReadingStatus.NORMAL -> Pair(R.color.status_normal_bg, getString(R.string.status_all_normal))
            ReadingStatus.WARNING -> Pair(R.color.status_warning_bg, getString(R.string.status_warnings_found))
            ReadingStatus.CRITICAL -> Pair(R.color.status_critical_bg, getString(R.string.status_critical_found))
            else -> Pair(R.color.status_unknown_bg, getString(R.string.status_unknown))
        }
        binding.bannerStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), bannerColor))
        binding.tvBannerText.text = bannerText

        adapter.submitList(report.validationResults)

        if (report.validationResults.isEmpty()) {
            binding.tvNoReadings.visibility = View.VISIBLE
            binding.rvReadings.visibility = View.GONE
        } else {
            binding.tvNoReadings.visibility = View.GONE
            binding.rvReadings.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.groupResults.visibility = View.GONE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.error_prefix, message)

        Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.retry) { viewModel.analyze(args.imageUri) }
            .show()
    }

    private fun translateEquipmentType(type: String): String = when (type.lowercase()) {
        "heater" -> "هيتر (سخان)"
        "separator" -> "فاصل (Separator)"
        "tank" -> "خزان"
        "compressor" -> "ضاغط (Compressor)"
        "pipeline" -> "خط أنابيب"
        "control_panel" -> "لوحة تحكم"
        else -> "معدات عامة"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
