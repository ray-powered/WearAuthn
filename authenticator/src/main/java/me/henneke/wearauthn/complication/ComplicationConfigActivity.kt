package me.henneke.wearauthn.complication

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.support.wearable.complications.ComplicationProviderService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import me.henneke.wearauthn.R
import me.henneke.wearauthn.bthid.canUseAuthenticator
import me.henneke.wearauthn.bthid.defaultAdapter
import me.henneke.wearauthn.bthid.identifier
import me.henneke.wearauthn.ui.hasBluetoothPermissions
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

class ComplicationConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val complicationId = intent.extras?.getInt(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID)
            ?: run {
                finish()
                return
            }

        val devices = getCompatibleDevices()

        setContent {
            WearAuthnTheme {
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
                            ListHeader {
                                Text(
                                    text = stringResource(R.string.preference_screen_title_shortcut_picker),
                                    style = MaterialTheme.typography.title3,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (devices.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.status_bluetooth_tap_and_pair),
                                    style = MaterialTheme.typography.body2,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        } else {
                            items(devices) { device ->
                                val iconRes = when (device.bluetoothClass?.majorDeviceClass) {
                                    BluetoothClass.Device.Major.AUDIO_VIDEO -> R.drawable.ic_btn_headset
                                    BluetoothClass.Device.Major.COMPUTER -> R.drawable.ic_btn_computer
                                    BluetoothClass.Device.Major.PHONE -> R.drawable.ic_btn_phone
                                    BluetoothClass.Device.Major.WEARABLE -> R.drawable.ic_btn_watch
                                    else -> R.drawable.ic_btn_bluetooth
                                }

                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        ShortcutComplicationProviderService.setDeviceShortcut(
                                            this@ComplicationConfigActivity,
                                            complicationId,
                                            device.address
                                        )
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                    },
                                    label = { Text(device.identifier) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(iconRes),
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
    private fun getCompatibleDevices(): List<BluetoothDevice> {
        val bonded = if (hasBluetoothPermissions) defaultAdapter.bondedDevices else emptySet()
        return bonded.filter { it.canUseAuthenticator }
    }
}


