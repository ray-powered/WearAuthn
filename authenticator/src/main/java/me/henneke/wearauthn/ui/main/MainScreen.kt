package me.henneke.wearauthn.ui.main

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.*
import me.henneke.wearauthn.LogLevel
import me.henneke.wearauthn.R

data class BondedDeviceUiModel(
    val device: BluetoothDevice,
    val name: String,
    val isCompatible: Boolean,
    val connectionState: Int,
    val majorClass: Int?
)

@Composable
fun MainScreen(
    listState: ScalingLazyListState,
    hasBluetoothPermissions: Boolean,
    onRequestBluetoothPermissions: () -> Unit,
    isBluetoothEnabled: Boolean,
    onRequestEnableBluetooth: () -> Unit,
    isDiscoverable: Boolean,
    onRequestMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    bondedDevices: List<BondedDeviceUiModel>,
    onConnectDevice: (BluetoothDevice) -> Unit,
    nfcState: Boolean?,
    onOpenNfcSettings: () -> Unit,
    userVerificationState: Boolean?,
    isScreenLockEnabled: Boolean,
    onEnablePasswordlessMode: () -> Unit,
    onManageCredentials: () -> Unit,
    onOpenAbout: () -> Unit,
    isDeveloperMode: Boolean,
    currentLogLevel: String,
    onSelectLogLevel: (String) -> Unit
) {
    var showLogLevelDialog by remember { mutableStateOf(false) }
    var showPasswordlessConfirmDialog by remember { mutableStateOf(false) }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Title Header
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.app_name),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bluetooth Permission Warning Chip
            if (!hasBluetoothPermissions) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestBluetoothPermissions,
                        label = { Text(stringResource(R.string.bluetooth_permissions_explanation)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_btn_bluetooth),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            // NFC Status Item
            item {
                when (nfcState) {
                    true -> {
                        Card(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.status_nfc_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    false -> {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenNfcSettings,
                            label = { Text(stringResource(R.string.preference_nfc_title)) },
                            secondaryLabel = { Text(stringResource(R.string.status_nfc_tap_and_enable)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_btn_settings),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = ChipDefaults.chipColors()
                        )
                    }
                    null -> {}
                }
            }

            // Bluetooth Section Header
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.preference_category_bluetooth_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (!isBluetoothEnabled) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestEnableBluetooth,
                        label = { Text(stringResource(R.string.preference_category_bluetooth_title)) },
                        secondaryLabel = { Text(stringResource(R.string.status_bluetooth_tap_to_enable)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_btn_bluetooth),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors()
                    )
                }
            } else {
                // Bluetooth Settings
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenBluetoothSettings,
                        label = { Text(stringResource(R.string.preference_bluetooth_settings_title)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_btn_settings),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors()
                    )
                }

                // Discoverable Toggle
                item {
                    SwitchButton(
                        modifier = Modifier.fillMaxWidth(),
                        checked = isDiscoverable,
                        onCheckedChange = { onRequestMakeDiscoverable() },
                        label = { Text(stringResource(R.string.preference_discoverable_title)) },
                        secondaryLabel = {
                            Text(
                                if (isDiscoverable) stringResource(R.string.preference_discoverable_summary_on)
                                else stringResource(R.string.preference_discoverable_summary_off)
                            )
                        }
                    )
                }

                // Bonded Devices List
                if (bondedDevices.isEmpty()) {
                    item {
                        Card(
                            onClick = onOpenBluetoothSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.status_bluetooth_tap_and_pair),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    items(bondedDevices) { model ->
                        val iconRes = when (model.majorClass) {
                            BluetoothClass.Device.Major.AUDIO_VIDEO -> R.drawable.ic_btn_headset
                            BluetoothClass.Device.Major.COMPUTER -> R.drawable.ic_btn_computer
                            BluetoothClass.Device.Major.PHONE -> R.drawable.ic_btn_phone
                            BluetoothClass.Device.Major.WEARABLE -> R.drawable.ic_btn_watch
                            else -> R.drawable.ic_btn_bluetooth
                        }

                        val statusText = when {
                            !model.isCompatible -> stringResource(R.string.status_bluetooth_use_via_nfc_instead)
                            model.connectionState == BluetoothProfile.STATE_CONNECTING -> stringResource(R.string.status_bluetooth_connecting)
                            model.connectionState == BluetoothProfile.STATE_CONNECTED -> stringResource(R.string.status_bluetooth_connected)
                            else -> null
                        }

                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { if (model.isCompatible) onConnectDevice(model.device) },
                            enabled = model.isCompatible && model.connectionState != BluetoothProfile.STATE_CONNECTING,
                            label = { Text(model.name) },
                            secondaryLabel = statusText?.let { { Text(it) } },
                            icon = {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = ChipDefaults.chipColors()
                        )
                    }
                }
            }

            // Advanced Section Header
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.preference_category_advanced_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Passwordless Mode (Single Factor Mode)
            item {
                val isChecked = userVerificationState == true
                val isEnabled = userVerificationState == false && isScreenLockEnabled
                val summaryText = when (userVerificationState) {
                    true -> stringResource(R.string.preference_single_factor_mode_summary_active)
                    false -> {
                        if (isScreenLockEnabled) stringResource(R.string.preference_single_factor_mode_summary_available)
                        else stringResource(R.string.preference_single_factor_mode_summary_enable_lock)
                    }
                    null -> stringResource(R.string.preference_single_factor_mode_summary_disabled)
                }

                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = isChecked,
                    onCheckedChange = {
                        if (isEnabled) {
                            showPasswordlessConfirmDialog = true
                        }
                    },
                    enabled = isEnabled,
                    label = { Text(stringResource(R.string.preference_single_factor_mode_title)) },
                    secondaryLabel = { Text(summaryText) }
                )
            }

            // Manage Credentials Chip
            item {
                val canManage = userVerificationState != false
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onManageCredentials,
                    enabled = canManage,
                    label = { Text(stringResource(R.string.preference_credential_management_title)) },
                    secondaryLabel = if (!canManage) {
                        { Text(stringResource(R.string.preference_manage_credentials_summary_disabled)) }
                    } else null,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_btn_key),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors()
                )
            }

            // About Chip
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenAbout,
                    label = { Text(stringResource(R.string.preference_about_title)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_btn_info),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors()
                )
            }

            // Developer Log Level (if enabled)
            if (isDeveloperMode) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showLogLevelDialog = true },
                        label = { Text(stringResource(R.string.preference_log_level_title)) },
                        secondaryLabel = { Text(currentLogLevel) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_btn_bug_report),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors()
                    )
                }
            }
        }
    }

    // Passwordless confirmation dialog
    if (showPasswordlessConfirmDialog) {
        val dialogListState = rememberScalingLazyListState()
        ScreenScaffold(scrollState = dialogListState) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = dialogListState,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = stringResource(R.string.prompt_single_factor_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.prompt_single_factor_mode_message).replace("<br/>", "\n").replace("<b>", "").replace("</b>", ""),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showPasswordlessConfirmDialog = false
                            onEnablePasswordlessMode()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.generic_accept))
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = { showPasswordlessConfirmDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.generic_deny))
                    }
                }
            }
        }
    }

    // Log level selection dialog
    if (showLogLevelDialog) {
        val logDialogListState = rememberScalingLazyListState()
        ScreenScaffold(scrollState = logDialogListState) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = logDialogListState,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ListHeader {
                        Text(
                            text = stringResource(R.string.preference_log_level_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(LogLevel.entries.reversed()) { level ->
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onSelectLogLevel(level.name)
                            showLogLevelDialog = false
                        },
                        label = { Text(level.name) },
                        colors = if (level.name == currentLogLevel) {
                            ChipDefaults.chipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else ChipDefaults.chipColors()
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = { showLogLevelDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.confirm_lock_cancel))
                    }
                }
            }
        }
    }
}

