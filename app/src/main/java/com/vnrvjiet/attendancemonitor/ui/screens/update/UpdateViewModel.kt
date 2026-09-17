package com.vnrvjiet.attendancemonitor.ui.screens.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.model.UpdateManifest
import com.vnrvjiet.attendancemonitor.data.repository.UpdateRepository
import com.vnrvjiet.attendancemonitor.data.repository.UpdateState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository.getInstance(application)

    val updateState: StateFlow<UpdateState> = repository.updateState

    fun checkForUpdate(manualCheck: Boolean = false) {
        viewModelScope.launch {
            repository.checkForUpdate(manualCheck)
        }
    }

    fun downloadAndInstall(manifest: UpdateManifest) {
        repository.downloadAndInstallUpdate(manifest)
    }

    fun dismissUpdate() {
        repository.dismissUpdate()
    }
}
