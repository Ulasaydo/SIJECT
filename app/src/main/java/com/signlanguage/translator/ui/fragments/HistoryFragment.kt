package com.signlanguage.translator.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.signlanguage.translator.R
import com.signlanguage.translator.data.model.WordHistory
import com.signlanguage.translator.databinding.FragmentHistoryBinding
import com.signlanguage.translator.ui.viewmodels.HistoryViewModel

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: HistoryViewModel by viewModels()
    private val historyAdapter = WordHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = historyAdapter
        binding.historyToolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.clearHistoryButton) {
                viewModel.clearHistory()
                true
            } else false
        }
        viewModel.history.observe(viewLifecycleOwner, ::renderHistory)
        viewModel.loadHistory()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun renderHistory(history: List<WordHistory>) {
        val isEmpty = history.isEmpty()
        binding.historyEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.historyRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        historyAdapter.submitHistory(history)
    }
}
