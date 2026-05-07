package com.signlanguage.translator.ui.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signlanguage.translator.data.model.WordHistory
import com.signlanguage.translator.databinding.ItemWordBinding
import com.signlanguage.translator.utils.asPercentText
import java.text.DateFormat
import java.util.Date

class WordHistoryAdapter : ListAdapter<WordHistory, WordHistoryAdapter.WordHistoryViewHolder>(DIFF_CALLBACK) {
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordHistoryViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitHistory(history: List<WordHistory>) {
        submitList(history)
    }

    inner class WordHistoryViewHolder(
        private val binding: ItemWordBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WordHistory) {
            binding.wordText.text = buildString {
                append(item.word)
                append("  ")
                append(item.confidence.asPercentText())
                append("\n")
                append(dateFormat.format(Date(item.timestampMillis)))
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WordHistory>() {
            override fun areItemsTheSame(oldItem: WordHistory, newItem: WordHistory): Boolean {
                return oldItem.timestampMillis == newItem.timestampMillis &&
                    oldItem.word == newItem.word
            }

            override fun areContentsTheSame(oldItem: WordHistory, newItem: WordHistory): Boolean {
                return oldItem == newItem
            }
        }
    }
}
