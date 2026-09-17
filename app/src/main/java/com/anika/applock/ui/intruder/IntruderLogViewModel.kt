package com.anika.applock.ui.intruder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class IntruderLogState(
    val attempts: List<IntruderAttempt> = emptyList(),
    val isLoading: Boolean = true
)

class IntruderLogViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(IntruderLogState())
    val state: StateFlow<IntruderLogState> = _state.asStateFlow()

    init {
        loadAttempts()
    }

    private fun loadAttempts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // TODO: Load from IntruderLogRepository
            // For now, scan the photos directory
            val photosDir = context.getExternalFilesDir(null)
            val photos = photosDir?.listFiles { file ->
                file.name.startsWith("intruder_") && file.extension == "jpg"
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            val attempts = photos.map { file ->
                IntruderAttempt(
                    timestamp = java.time.Instant.ofEpochMilli(file.lastModified()),
                    targetApp = "Unknown",  // TODO: load from metadata
                    photoPath = file.absolutePath
                )
            }

            _state.value = _state.value.copy(
                attempts = attempts,
                isLoading = false
            )
        }
    }

    fun deleteAttempt(attempt: IntruderAttempt) {
        viewModelScope.launch {
            attempt.photoPath?.let { path ->
                File(path).delete()
            }
            loadAttempts()
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            val photosDir = context.getExternalFilesDir(null)
            photosDir?.listFiles { file ->
                file.name.startsWith("intruder_") && file.extension == "jpg"
            }?.forEach { it.delete() }
            loadAttempts()
        }
    }
}
