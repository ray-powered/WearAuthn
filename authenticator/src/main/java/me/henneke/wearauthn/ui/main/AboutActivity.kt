package me.henneke.wearauthn.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.henneke.wearauthn.BuildConfig
import me.henneke.wearauthn.R
import me.henneke.wearauthn.fido.context.checkAllKeysInHardware
import me.henneke.wearauthn.ui.openUrlOnPhone
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearAuthnTheme {
                val listState = rememberScalingLazyListState()
                var keyStorageRes by remember { mutableStateOf(R.string.message_key_storage_type_unknown) }
                var textDialogContent by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val inHardware = withContext(Dispatchers.IO) {
                        checkAllKeysInHardware()
                    }
                    keyStorageRes = if (inHardware) {
                        R.string.message_key_storage_type_hardware
                    } else {
                        R.string.message_key_storage_type_software
                    }
                }

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
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.title2,
                                    color = MaterialTheme.colors.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        item {
                            Text(
                                text = BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.caption1,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.about_how_to_use).replace("<b>", "").replace("</b>", ""),
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }
                        }
                        item {
                            Chip(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    openUrlOnPhone(this@AboutActivity, getString(R.string.url_usage))
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
                        item {
                            Card(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(keyStorageRes),
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        item {
                            Chip(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { textDialogContent = privacyPolicy },
                                label = { Text(stringResource(R.string.label_privacy)) },
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                        item {
                            Chip(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { textDialogContent = licensesText },
                                label = { Text(stringResource(R.string.label_licenses)) },
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }

                // Full text viewer dialog
                textDialogContent?.let { content ->
                    val textListState = rememberScalingLazyListState()
                    Dialog(
                        showDialog = textDialogContent != null,
                        onDismissRequest = { textDialogContent = null },
                        scrollState = textListState
                    ) {
                        ScalingLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            state = textListState,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                CompactChip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { textDialogContent = null },
                                    label = { Text(stringResource(R.string.confirm_lock_continue)) },
                                    colors = ChipDefaults.primaryChipColors()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val privacyPolicy by lazy {
        resources.openRawResource(R.raw.privacy_policy).bufferedReader().use {
            it.readText()
        }
    }

    private val licensesText by lazy {
        resources.openRawResource(R.raw.licenses_text).bufferedReader().use {
            it.readText()
        }
    }
}


