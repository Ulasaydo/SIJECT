package com.signlanguage.translator.ui.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.signlanguage.translator.data.model.WordHistory
import com.signlanguage.translator.databinding.ItemWordBinding
import com.signlanguage.translator.utils.asPercentText
import java.text.DateFormat
import java.util.Date

class WordHistoryAdapter : RecyclerView.Adapter<WordHistoryAdapter.WordHistoryViewHolder>() {
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private val items = mutableListOf<WordHistory>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordHistoryViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordHistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitHistory(history: List<WordHistory>) {
        items.clear()
        items.addAll(history)
        notifyDataSetChanged()
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
}
