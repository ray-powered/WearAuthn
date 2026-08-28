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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import me.henneke.wearauthn.R
import me.henneke.wearauthn.fido.context.AuthenticatorContext
import me.henneke.wearauthn.fido.context.WebAuthnCredential
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

@ExperimentalUnsignedTypes
class ResidentCredentialsList : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearAuthnTheme {
                val listState = rememberScalingLazyListState()
                var credentialsMap by remember {
                    mutableStateOf(AuthenticatorContext.getAllResidentCredentials(this@ResidentCredentialsList))
                }
                var selectedCredential by remember { mutableStateOf<Pair<String, WebAuthnCredential>?>(null) }
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                                    text = if (credentialsMap.isEmpty()) {
                                        stringResource(R.string.credential_management_title_no_credentials)
                                    } else {
                                        stringResource(R.string.credential_management_title)
                                    },
                                    style = MaterialTheme.typography.title3,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        for ((rpId, credentials) in credentialsMap) {
                            if (credentials.isEmpty()) continue

                            item {
                                ListHeader {
                                    Text(
                                        text = rpId,
                                        style = MaterialTheme.typography.caption1,
                                        color = MaterialTheme.colors.secondary
                                    )
                                }
                            }

                            items(credentials) { credential ->
                                credential.unlockUserInfoIfNecessary()
                                val twoLineInfo = credential.getTwoLineInfo(1)
                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        selectedCredential = Pair(rpId, credential)
                                    },
                                    label = { Text(twoLineInfo.first) },
                                    secondaryLabel = twoLineInfo.second?.let { { Text(it) } },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_btn_key),
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

                // Credential details dialog
                selectedCredential?.let { (rpId, credential) ->
                    val info = credential.getFormattedInfo() ?: ""
                    val dialogListState = rememberScalingLazyListState()
                    Dialog(
                        showDialog = selectedCredential != null && !showDeleteConfirmDialog,
                        onDismissRequest = { selectedCredential = null },
                        scrollState = dialogListState
                    ) {
                        ScalingLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            state = dialogListState,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = rpId,
                                    style = MaterialTheme.typography.title2,
                                    color = MaterialTheme.colors.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { showDeleteConfirmDialog = true },
                                    colors = ChipDefaults.chipColors(
                                        backgroundColor = MaterialTheme.colors.error,
                                        contentColor = MaterialTheme.colors.onError
                                    ),
                                    label = { Text(stringResource(R.string.button_delete)) }
                                )
                            }
                            item {
                                CompactChip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { selectedCredential = null },
                                    label = { Text(stringResource(R.string.confirm_lock_cancel)) },
                                    colors = ChipDefaults.secondaryChipColors()
                                )
                            }
                        }
                    }

                    // Delete confirmation dialog
                    if (showDeleteConfirmDialog) {
                        val confirmListState = rememberScalingLazyListState()
                        Dialog(
                            showDialog = showDeleteConfirmDialog,
                            onDismissRequest = { showDeleteConfirmDialog = false },
                            scrollState = confirmListState
                        ) {
                            Alert(
                                scrollState = confirmListState,
                                title = {
                                    Text(
                                        text = stringResource(
                                            R.string.prompt_delete_resident_credential_message,
                                            credential.getTwoLineInfo(1).first
                                        ),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.error
                                    )
                                },
                                negativeButton = {
                                    Button(
                                        onClick = { showDeleteConfirmDialog = false },
                                        colors = ButtonDefaults.secondaryButtonColors()
                                    ) {
                                        Text(stringResource(R.string.confirm_lock_cancel))
                                    }
                                },
                                positiveButton = {
                                    Button(
                                        onClick = {
                                            AuthenticatorContext.deleteResidentCredential(
                                                this@ResidentCredentialsList,
                                                credential
                                            )
                                            credentialsMap = AuthenticatorContext.getAllResidentCredentials(this@ResidentCredentialsList)
                                            showDeleteConfirmDialog = false
                                            selectedCredential = null
                                        },
                                        colors = ButtonDefaults.primaryButtonColors()
                                    ) {
                                        Text(stringResource(R.string.button_delete))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}