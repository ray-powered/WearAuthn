package me.henneke.wearauthn.sync

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import me.henneke.wearauthn.complication.ShortcutComplicationProviderService

class UnlockComplicationListenerService : WearableListenerService() {
    @ExperimentalUnsignedTypes
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Complication is always enabled and permanently free for all users.
    }

    companion object {
        fun isComplicationEnabled(context: Context): Boolean {
            val component =
                ComponentName(context, ShortcutComplicationProviderService::class.java)
            val currentState = context.packageManager.getComponentEnabledSetting(component)
            if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                try {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }
            return true
        }
    }
}