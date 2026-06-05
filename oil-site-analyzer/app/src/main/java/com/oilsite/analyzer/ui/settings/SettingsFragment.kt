package com.oilsite.analyzer.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.oilsite.analyzer.R
import com.oilsite.analyzer.data.repository.AnalysisRepository
import com.oilsite.analyzer.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AnalysisRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AnalysisRepository(requireContext())

        // Show masked key if already saved
        if (repository.hasApiKey()) {
            binding.tvKeyStatus.text = getString(R.string.api_key_saved)
            binding.tvKeyStatus.setTextColor(resources.getColor(R.color.status_normal_text, null))
        } else {
            binding.tvKeyStatus.text = getString(R.string.api_key_not_set)
            binding.tvKeyStatus.setTextColor(resources.getColor(R.color.status_warning_text, null))
        }

        binding.btnSaveKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            if (key.length < 10) {
                binding.tilApiKey.error = getString(R.string.api_key_invalid)
                return@setOnClickListener
            }
            binding.tilApiKey.error = null
            repository.saveApiKey(key)
            binding.etApiKey.setText("")
            binding.tvKeyStatus.text = getString(R.string.api_key_saved)
            binding.tvKeyStatus.setTextColor(resources.getColor(R.color.status_normal_text, null))
            Toast.makeText(requireContext(), R.string.api_key_saved_toast, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
