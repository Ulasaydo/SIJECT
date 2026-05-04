package com.signlanguage.translator.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.signlanguage.translator.data.model.WordHistory
import com.signlanguage.translator.data.repository.LocalDataRepository

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val localDataRepository = LocalDataRepository(application)
    private val _history = MutableLiveData<List<WordHistory>>()
    val history: LiveData<List<WordHistory>> = _history

    fun loadHistory() {
        _history.value = localDataRepository.getWordHistory()
    }

    fun clearHistory() {
        localDataRepository.clearHistory()
        _history.value = emptyList()
    }
}
