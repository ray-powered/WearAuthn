package me.henneke.wearauthn.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ProviderUpdateRequester
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.common.util.Hex
import me.henneke.wearauthn.Logging
import me.henneke.wearauthn.R
import me.henneke.wearauthn.bthid.HidDataSender
import me.henneke.wearauthn.bthid.HidDeviceProfile
import me.henneke.wearauthn.bthid.HidIntrDataListener
import me.henneke.wearauthn.bthid.InputHostWrapper
import me.henneke.wearauthn.bthid.defaultAdapter
import me.henneke.wearauthn.bthid.identifier
import me.henneke.wearauthn.bthid.isBluetoothEnabled
import me.henneke.wearauthn.complication.ShortcutComplicationProviderService
import me.henneke.wearauthn.d
import me.henneke.wearauthn.e
import me.henneke.wearauthn.fido.context.AuthenticatorStatus
import me.henneke.wearauthn.fido.hid.TransactionManager
import me.henneke.wearauthn.i
import me.henneke.wearauthn.ui.WearBodyItem
import me.henneke.wearauthn.ui.WearButton
import me.henneke.wearauthn.ui.WearListScreen
import me.henneke.wearauthn.ui.openUrlOnPhone
import me.henneke.wearauthn.ui.theme.WearAuthnTheme
import me.henneke.wearauthn.v
import me.henneke.wearauthn.w
import java.util.Date

@ExperimentalUnsignedTypes
class AuthenticatorAttachedActivity : ComponentActivity(), Logging {

    override val TAG = "AuthenticatorAttachedActivity"

    private var transactionManager: TransactionManager? = null
    private var hidDeviceProfile: HidDeviceProfile? = null
    private lateinit var authenticatorContext: HidAuthenticatorContext
    private var connectionText by mutableStateOf("")
    private var isAmbient by mutableStateOf(false)
    private var ambientTime by mutableStateOf("")

    private val ambientObserver by lazy {
        AmbientLifecycleObserver(
            this,
            object : AmbientLifecycleObserver.AmbientLifecycleCallback {
                override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                    isAmbient = true
                    updateTime()
                }

                override fun onUpdateAmbient() = updateTime()

                override fun onExitAmbient() {
                    isAmbient = false
                }
            },
        )
    }

    private val hidIntrDataListener = object : HidIntrDataListener {
        override fun onIntrData(
            device: BluetoothDevice,
            reportId: Byte,
            data: ByteArray,
            host: InputHostWrapper,
        ) {
            i { "Received report" }
            d { "Report ID: $reportId" }
            v { "<- ${Hex.bytesToStringUppercase(data)}" }
            transactionManager?.handleReport(data) {
                for (rawReport in it) {
                    v { "-> ${Hex.bytesToStringUppercase(rawReport)}" }
                    host.sendReport(device, reportId.toInt(), rawReport)
                }
            }
        }
    }

    private val hidProfileListener = object : HidDataSender.ProfileListener {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            runOnUiThread {
                connectionText = when (state) {
                    BluetoothProfile.STATE_CONNECTING -> getString(
                        R.string.connecting_to_device_plain,
                        device.identifier,
                    )
                    BluetoothProfile.STATE_CONNECTED -> getString(
                        R.string.connected_to_device_plain,
                        device.identifier,
                    )
                    else -> connectionText
                }
                if (state == BluetoothProfile.STATE_DISCONNECTING ||
                    state == BluetoothProfile.STATE_DISCONNECTED
                ) finish()
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            if (!registered) finish()
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        setContent {
            WearAuthnTheme {
                if (isAmbient) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = ambientTime, style = MaterialTheme.typography.displayMedium)
                    }
                } else {
                    WearListScreen(title = connectionText.ifEmpty { stringResource(R.string.app_name) }) {
                        item {
                            WearBodyItem(
                                text = stringResource(R.string.connected_to_device_explanation),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        item {
                            WearButton(
                                label = stringResource(R.string.message_continue_on_phone),
                                iconRes = R.drawable.ic_btn_open_on_phone,
                                onClick = { openUrlOnPhone(this@AuthenticatorAttachedActivity, getString(R.string.url_setup)) },
                            )
                        }
                    }
                }
            }
        }
        authenticatorContext = HidAuthenticatorContext(this)
        hidDeviceProfile = HidDataSender.register(this, hidProfileListener, hidIntrDataListener)
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        if (!isBluetoothEnabled) startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        transactionManager = TransactionManager(authenticatorContext)
        val profile = hidDeviceProfile ?: run {
            e { "hidDeviceProfile is null" }
            finish()
            return
        }
        if (profile.connectedDevices.isEmpty() && intent.hasExtra(EXTRA_DEVICE)) {
            if (intent.hasExtra(ComplicationProviderService.EXTRA_COMPLICATION_ID)) {
                ProviderUpdateRequester(
                    this,
                    ComponentName(this, ShortcutComplicationProviderService::class.java),
                ).requestUpdateAll()
            }
            val device = intent.bluetoothDeviceExtra(EXTRA_DEVICE)
            if (device == null || device !in defaultAdapter.bondedDevices) {
                startActivity(Intent(this, AuthenticatorActivity::class.java))
                finish()
                return
            }
            hidProfileListener.onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTING)
            HidDataSender.requestConnect(device)
        } else if (profile.connectedDevices.isEmpty()) {
            finish()
        } else {
            val connectedDevice = profile.connectedDevices.first()
            hidProfileListener.onConnectionStateChanged(connectedDevice, BluetoothProfile.STATE_CONNECTED)
        }
    }

    override fun onStop() {
        super.onStop()
        if (authenticatorContext.status != AuthenticatorStatus.IDLE) {
            w { "onStop() called during authenticator action" }
            return
        }
        HidDataSender.requestConnect(null)
        transactionManager = null
    }

    override fun onDestroy() {
        HidDataSender.unregister(hidProfileListener, hidIntrDataListener)
        super.onDestroy()
    }

    private fun updateTime() {
        ambientTime = DateFormat.getTimeFormat(this).format(Date())
    }

    companion object {
        const val EXTRA_DEVICE = "me.henneke.wearauthn.extra.DEVICE"
    }
}

@Suppress("DEPRECATION")
private fun Intent.bluetoothDeviceExtra(key: String): BluetoothDevice? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, BluetoothDevice::class.java)
    } else {
        getParcelableExtra(key)
    }
