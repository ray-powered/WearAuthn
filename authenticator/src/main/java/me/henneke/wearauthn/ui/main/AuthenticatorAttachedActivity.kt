package me.henneke.wearauthn.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ProviderUpdateRequester
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
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
import me.henneke.wearauthn.ui.openUrlOnPhone
import me.henneke.wearauthn.ui.theme.WearAuthnTheme
import me.henneke.wearauthn.v
import me.henneke.wearauthn.w
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@ExperimentalUnsignedTypes
class AuthenticatorAttachedActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private var transactionManager: TransactionManager? = null
    private var hidDeviceProfile: HidDeviceProfile? = null
    private lateinit var authenticatorContext: HidAuthenticatorContext
    private var ambientController: AmbientModeSupport.AmbientController? = null

    private val connectedDeviceNameState = mutableStateOf("")
    private val isConnectingState = mutableStateOf(false)
    private val isAmbientState = mutableStateOf(false)
    private val ambientTimeState = mutableStateOf("")

    private val hidIntrDataListener = object : HidIntrDataListener {
        override fun onIntrData(
            device: BluetoothDevice,
            reportId: Byte,
            data: ByteArray,
            host: InputHostWrapper
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
            when (state) {
                BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED -> {
                    i { "Disconnecting; finishing" }
                    finish()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    i { "Connecting..." }
                    isConnectingState.value = true
                    connectedDeviceNameState.value = device.identifier
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    i { "Connected" }
                    isConnectingState.value = false
                    connectedDeviceNameState.value = device.identifier
                }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            if (!registered) {
                i { "App no longer registered; finishing" }
                finish()
            }
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ambientController = AmbientModeSupport.attach(this)
        } catch (_: Exception) {}

        authenticatorContext = HidAuthenticatorContext(this)
        hidDeviceProfile = HidDataSender.register(this, hidProfileListener, hidIntrDataListener)

        setContent {
            WearAuthnTheme {
                val isAmbient by isAmbientState
                val ambientTime by ambientTimeState
                val deviceName by connectedDeviceNameState
                val isConnecting by isConnectingState

                if (isAmbient) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ambientTime,
                            style = MaterialTheme.typography.display1,
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                } else {
                    val listState = rememberScalingLazyListState()
                    Scaffold(
                        positionIndicator = {
                            PositionIndicator(scalingLazyListState = listState)
                        }
                    ) {
                        ScalingLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            state = listState,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Icon(
                                    painter = painterResource(R.drawable.ic_btn_bluetooth),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isConnecting) {
                                        "Attaching to\n$deviceName…"
                                    } else {
                                        "Attached to\n$deviceName"
                                    },
                                    style = MaterialTheme.typography.title2,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colors.onBackground
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.connected_to_device_explanation),
                                    style = MaterialTheme.typography.body2,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        openUrlOnPhone(this@AuthenticatorAttachedActivity, getString(R.string.url_setup))
                                    },
                                    label = { Text(stringResource(R.string.message_continue_on_phone)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_btn_open_on_phone),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    colors = ChipDefaults.secondaryChipColors()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()

        if (!isBluetoothEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
        }

        transactionManager = TransactionManager(authenticatorContext)
        if (hidDeviceProfile == null) {
            e { "hidDeviceProfile is null" }
            finish()
            return
        }

        if (hidDeviceProfile!!.connectedDevices.isEmpty() && intent.hasExtra(EXTRA_DEVICE)) {
            if (intent.hasExtra(ComplicationProviderService.EXTRA_COMPLICATION_ID)) {
                i { "Updating complication" }
                ProviderUpdateRequester(
                    this,
                    ComponentName(this, ShortcutComplicationProviderService::class.java)
                ).requestUpdateAll()
            }
            val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
            if (device == null || device !in defaultAdapter.bondedDevices) {
                i { "No device extra or no longer bonded; finishing" }
                startActivity(Intent(this, AuthenticatorActivity::class.java))
                finish()
                return
            }
            hidProfileListener.onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTING)
            HidDataSender.requestConnect(device)
        } else if (hidDeviceProfile!!.connectedDevices.isEmpty()) {
            e { "Started without connected device or device extra; finishing" }
            finish()
        } else {
            check(hidDeviceProfile!!.connectedDevices.size == 1)
            val connectedDevice = hidDeviceProfile!!.connectedDevices[0]
            hidProfileListener.onConnectionStateChanged(
                connectedDevice,
                BluetoothProfile.STATE_CONNECTED
            )
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
        super.onDestroy()
        HidDataSender.unregister(hidProfileListener, hidIntrDataListener)
    }

    private fun updateTime() {
        ambientTimeState.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback =
        object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                isAmbientState.value = true
                updateTime()
            }

            override fun onUpdateAmbient() {
                updateTime()
            }

            override fun onExitAmbient() {
                isAmbientState.value = false
            }
        }

    companion object : Logging {
        override val TAG = "AuthenticatorAttachedActivity"
        const val EXTRA_DEVICE = "me.henneke.wearauthn.extra.DEVICE"
    }
}

