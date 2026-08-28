package me.henneke.wearauthn.companion.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.*
import kotlinx.coroutines.cancel

class MainViewModel(application: Application) : AndroidViewModel(application),
    CapabilityClient.OnCapabilityChangedListener {

    private val capabilityClient = Wearable.getCapabilityClient(application.applicationContext)

    private val _isWatchAppInstalled = MutableLiveData(false)
    val isWatchAppInstalled: LiveData<Boolean> = _isWatchAppInstalled

    init {
        capabilityClient.addListener(
            this,
            "wearauthn-watch"
        )
        queryWatchAppInstalled()
    }

    fun update() {
        queryWatchAppInstalled()
    }

    private fun queryWatchAppInstalled() {
        capabilityClient.getCapability("wearauthn-watch", CapabilityClient.FILTER_ALL).apply {
            addOnSuccessListener { info ->
                _isWatchAppInstalled.postValue(info.nodes.isNotEmpty())
            }
            addOnFailureListener {
                // If capability check fails, also check connected nodes
                Wearable.getNodeClient(getApplication<Application>().applicationContext).connectedNodes.addOnSuccessListener { nodes ->
                    _isWatchAppInstalled.postValue(nodes.isNotEmpty())
                }
            }
        }
    }

    override fun onCapabilityChanged(info: CapabilityInfo) {
        queryWatchAppInstalled()
    }

    override fun onCleared() {
        capabilityClient.removeListener(this, "wearauthn-watch")
        viewModelScope.coroutineContext.cancel()
    }
}

