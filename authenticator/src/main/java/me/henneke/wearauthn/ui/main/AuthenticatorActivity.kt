package me.henneke.wearauthn.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import kotlinx.coroutines.*
import me.henneke.wearauthn.BuildConfig
import me.henneke.wearauthn.LogLevel
import me.henneke.wearauthn.Logging
import me.henneke.wearauthn.R
import me.henneke.wearauthn.bthid.HidDataSender
import me.henneke.wearauthn.bthid.HidDeviceProfile
import me.henneke.wearauthn.bthid.canUseAuthenticator
import me.henneke.wearauthn.bthid.canUseAuthenticatorViaBluetooth
import me.henneke.wearauthn.bthid.identifier
import me.henneke.wearauthn.fido.context.AuthenticatorContext
import me.henneke.wearauthn.fido.context.armUserVerificationFuse
import me.henneke.wearauthn.fido.context.getUserVerificationState
import me.henneke.wearauthn.isDeveloperModeEnabled
import me.henneke.wearauthn.ui.ConfirmDeviceCredentialActivity
import me.henneke.wearauthn.ui.EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER
import me.henneke.wearauthn.ui.bluetoothAdapter
import me.henneke.wearauthn.ui.defaultSharedPreferences
import me.henneke.wearauthn.ui.hasBluetoothPermissions
import me.henneke.wearauthn.ui.theme.WearAuthnTheme
import kotlin.coroutines.CoroutineContext

@ExperimentalUnsignedTypes
class AuthenticatorActivity : FragmentActivity(), CoroutineScope, Logging,
    AmbientModeSupport.AmbientCallbackProvider {

    override val TAG = "AuthenticatorActivity"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    private var hidDeviceProfile: HidDeviceProfile? = null
    private var ambientController: AmbientModeSupport.AmbientController? = null
    private var hasUpdatedInAmbientMode = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshState()
        registerHidDeviceProfile()
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshState()
    }

    private val makeDiscoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshState()
    }

    // State holders for Compose
    private val hasBtPermissionsState = mutableStateOf(false)
    private val isBtEnabledState = mutableStateOf(false)
    private val isDiscoverableState = mutableStateOf(false)
    private val bondedDevicesState = mutableStateListOf<BondedDeviceUiModel>()
    private val nfcStateHolder = mutableStateOf<Boolean?>(null)
    private val userVerificationStateHolder = mutableStateOf<Boolean?>(null)
    private val isScreenLockEnabledState = mutableStateOf(false)
    private val isDevModeState = mutableStateOf(false)
    private val currentLogLevelState = mutableStateOf(LogLevel.Disabled.name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ambientController = AmbientModeSupport.attach(this)

        setContent {
            WearAuthnTheme {
                val listState = rememberScalingLazyListState()
                val hasPerms by hasBtPermissionsState
                val isBtEnabled by isBtEnabledState
                val isDiscoverable by isDiscoverableState
                val nfcState by nfcStateHolder
                val uvState by userVerificationStateHolder
                val isLockEnabled by isScreenLockEnabledState
                val isDevMode by isDevModeState
                val logLevel by currentLogLevelState

                MainScreen(
                    listState = listState,
                    hasBluetoothPermissions = hasPerms,
                    onRequestBluetoothPermissions = { requestBluetoothPermissions() },
                    isBluetoothEnabled = isBtEnabled,
                    onRequestEnableBluetooth = {
                        enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    },
                    isDiscoverable = isDiscoverable,
                    onRequestMakeDiscoverable = {
                        makeDiscoverableLauncher.launch(
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60)
                            }
                        )
                    },
                    onOpenBluetoothSettings = {
                        startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    bondedDevices = bondedDevicesState,
                    onConnectDevice = { device ->
                        HidDataSender.requestConnect(device)
                    },
                    nfcState = nfcState,
                    onOpenNfcSettings = {
                        startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                    },
                    userVerificationState = uvState,
                    isScreenLockEnabled = isLockEnabled,
                    onEnablePasswordlessMode = {
                        enablePasswordlessMode()
                    },
                    onManageCredentials = {
                        manageCredentials()
                    },
                    onOpenAbout = {
                        startActivity(Intent(this@AuthenticatorActivity, AboutActivity::class.java))
                    },
                    isDeveloperMode = isDevMode,
                    currentLogLevel = logLevel,
                    onSelectLogLevel = { level ->
                        Logging.init(applicationContext, level)
                        defaultSharedPreferences.edit {
                            putString(getString(R.string.preference_log_level_key), level)
                        }
                        currentLogLevelState.value = level
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            bluetoothBroadcastReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_CLASS_CHANGED)
                addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            }
        )
        registerHidDeviceProfile()
        refreshState()
        launch {
            AuthenticatorContext.initAuthenticator(this@AuthenticatorActivity.applicationContext)
            AuthenticatorContext.refreshCachedWebAuthnCredentialIfNecessary(applicationContext)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(bluetoothBroadcastReceiver)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        if (hidDeviceProfile != null) {
            HidDataSender.unregister(hidProfileListener, null)
            hidDeviceProfile = null
        }
    }

    private fun registerHidDeviceProfile() {
        if (hasBluetoothPermissions && bluetoothAdapter != null && hidDeviceProfile == null) {
            hidDeviceProfile = HidDataSender.register(this, hidProfileListener, null)
        }
    }

    private fun requestBluetoothPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun refreshState() {
        val hasPerms = hasBluetoothPermissions
        hasBtPermissionsState.value = hasPerms

        val btAdapter = bluetoothAdapter
        val isBtOn = btAdapter?.state == BluetoothAdapter.STATE_ON
        isBtEnabledState.value = isBtOn

        if (hasPerms && isBtOn && btAdapter != null) {
            val scanMode = btAdapter.scanMode
            isDiscoverableState.value = scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE

            val devices = btAdapter.bondedDevices ?: emptySet()
            bondedDevicesState.clear()
            for (device in devices) {
                if (device.canUseAuthenticator) {
                    val connState = hidDeviceProfile?.getConnectionState(device)
                        ?: BluetoothProfile.STATE_DISCONNECTED
                    bondedDevicesState.add(
                        BondedDeviceUiModel(
                            device = device,
                            name = device.identifier,
                            isCompatible = device.canUseAuthenticatorViaBluetooth,
                            connectionState = connState,
                            majorClass = device.bluetoothClass?.majorDeviceClass
                        )
                    )
                }
            }
        } else {
            isDiscoverableState.value = false
            bondedDevicesState.clear()
        }

        // NFC State
        val nfcManager = getSystemService(NfcManager::class.java)
        nfcStateHolder.value = nfcManager?.defaultAdapter?.isEnabled

        // User verification state
        userVerificationStateHolder.value = getUserVerificationState(this)
        isScreenLockEnabledState.value = AuthenticatorContext.isScreenLockEnabled(this)

        // Developer mode & log level
        isDevModeState.value = isDeveloperModeEnabled
        currentLogLevelState.value = defaultSharedPreferences.getString(
            getString(R.string.preference_log_level_key),
            LogLevel.Disabled.name
        ) ?: LogLevel.Disabled.name
    }

    private fun enablePasswordlessMode() {
        val intent = Intent(this, ConfirmDeviceCredentialActivity::class.java).apply {
            putExtra(
                EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER,
                object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        if (resultCode == Activity.RESULT_OK) {
                            armUserVerificationFuse(this@AuthenticatorActivity)
                            refreshState()
                        }
                    }
                }
            )
        }
        startActivity(intent)
    }

    private fun manageCredentials() {
        val intent = Intent(this, ConfirmDeviceCredentialActivity::class.java).apply {
            putExtra(
                EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER,
                object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        if (resultCode == Activity.RESULT_OK) {
                            startActivity(
                                Intent(this@AuthenticatorActivity, ResidentCredentialsList::class.java)
                            )
                        }
                    }
                }
            )
        }
        startActivity(intent)
    }

    private val hidProfileListener = object : HidDataSender.ProfileListener {
        override fun onAppStatusChanged(registered: Boolean) {
            if (!registered) finish()
            refreshState()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            refreshState()
            if (state == BluetoothProfile.STATE_CONNECTED) {
                startActivity(
                    Intent(this@AuthenticatorActivity, AuthenticatorAttachedActivity::class.java)
                )
            }
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            refreshState()
        }
    }

    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            refreshState()
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback =
        object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                hasUpdatedInAmbientMode = false
            }

            override fun onUpdateAmbient() {
                if (hasUpdatedInAmbientMode) {
                    finish()
                } else {
                    hasUpdatedInAmbientMode = true
                }
            }
        }
}

